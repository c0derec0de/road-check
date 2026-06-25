import os
import pandas as pd
import numpy as np
from sqlalchemy import create_engine, text
from sklearn.cluster import DBSCAN
from datetime import datetime, timedelta
import joblib
from math import radians, sin, cos, sqrt, atan2


DB_HOST = os.getenv('DB_HOST', 'roadcheck-postgres')
DB_PORT = os.getenv('DB_PORT', '5432')
DB_NAME = os.getenv('DB_NAME', 'roadcheck')
DB_USER = os.getenv('DB_USER', 'roadcheck')
DB_PASS = os.getenv('DB_PASS', 'roadcheck')


def convert_numpy_types(obj):
    if isinstance(obj, np.integer):
        return int(obj)
    elif isinstance(obj, np.floating):
        return float(obj)
    elif isinstance(obj, np.ndarray):
        return obj.tolist()
    elif isinstance(obj, pd.Timestamp):
        return obj.to_pydatetime()
    elif isinstance(obj, dict):
        return {key: convert_numpy_types(value) for key, value in obj.items()}
    elif isinstance(obj, list):
        return [convert_numpy_types(item) for item in obj]
    elif isinstance(obj, tuple):
        return tuple(convert_numpy_types(item) for item in obj)
    else:
        return obj


def get_engine():
    from urllib.parse import quote_plus
    encoded_pass = quote_plus(DB_PASS)
    url = f"postgresql://{DB_USER}:{encoded_pass}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
    return create_engine(url)


def haversine(lat1, lon1, lat2, lon2):
    R = 6371000
    
    lat1_rad = radians(lat1)
    lat2_rad = radians(lat2)
    delta_lat = radians(lat2 - lat1)
    delta_lon = radians(lon2 - lon1)
    
    a = sin(delta_lat/2)**2 + cos(lat1_rad) * cos(lat2_rad) * sin(delta_lon/2)**2
    c = 2 * atan2(sqrt(a), sqrt(1-a))
    
    return R * c


def load_incidents():
    engine = get_engine()
    
    query = """
    SELECT 
        id,
        incident_type,
        latitude,
        longitude,
        description,
        created_at,
        fatalities,
        injuries,
        cause
    FROM reports
    WHERE latitude IS NOT NULL 
      AND longitude IS NOT NULL
      AND created_at >= NOW() - INTERVAL '365 days'
    """
    
    df = pd.read_sql(query, engine, parse_dates=['created_at'])
    
    return df


def load_weather():
    engine = get_engine()
    
    query = """
    SELECT 
        timestamp,
        humidity,
        temperature,
        wind_direction,
        wind_speed,
        cloud_cover,
        visibility,
        dew_point,
        precipitation,
        current_weather,
        past_weather_1,
        past_weather_2,
        cloud_height,
        latitude,
        longitude
    FROM weather
    WHERE timestamp >= NOW() - INTERVAL '365 days'
    ORDER BY timestamp
    """
    
    df = pd.read_sql(query, engine, parse_dates=['timestamp'])
    
    return df


def cluster_incidents(df, eps_meters=3000, min_samples=2):
    if len(df) == 0:
        return df, []
    

    coords = df[['latitude', 'longitude']].values
    

    coords_rad = np.radians(coords)
    

    kms_per_radian = 6371.0088
    eps_rad = eps_meters / (kms_per_radian * 1000)
    

    clustering = DBSCAN(eps=eps_rad, min_samples=min_samples, metric='haversine')
    labels = clustering.fit_predict(coords_rad)
    
    df['cluster'] = labels
    n_clusters = len(set(labels)) - (1 if -1 in labels else 0)
    n_noise = list(labels).count(-1)
    
    
    if n_clusters > 0:

        for cluster_id in set(labels):
            if cluster_id != -1:
                cluster_size = len(df[df['cluster'] == cluster_id])
               
    
    return df, clustering


def get_weather_for_zone(weather_df, center_lat, center_lng, radius_meters, incident_times):
    if len(weather_df) == 0 or len(incident_times) == 0:
        return {}
    

    nearby_weather = []
    for _, row in weather_df.iterrows():
        if pd.notna(row['latitude']) and pd.notna(row['longitude']):
            dist = haversine(center_lat, center_lng, row['latitude'], row['longitude'])
            if dist <= radius_meters * 3:
                nearby_weather.append(row)
    
    if not nearby_weather:
        return {}
    
    weather_nearby_df = pd.DataFrame(nearby_weather)
    

    weather_stats = {
        'avg_temperature': weather_nearby_df['temperature'].mean(),
        'avg_humidity': weather_nearby_df['humidity'].mean(),
        'avg_wind_speed': weather_nearby_df['wind_speed'].mean(),
        'avg_precipitation': weather_nearby_df['precipitation'].mean(),
        'avg_visibility': weather_nearby_df['visibility'].mean(),
        'avg_cloud_cover': weather_nearby_df['cloud_cover'].mean(),
    }
    

    incident_weather = []
    for incident_time in incident_times:

        time_diff = abs(weather_nearby_df['timestamp'] - incident_time)
        if len(time_diff) > 0:
            closest_idx = time_diff.idxmin()
            if time_diff.min() <= timedelta(hours=6):
                incident_weather.append(weather_nearby_df.loc[closest_idx])
    
    if incident_weather:
        incident_weather_df = pd.DataFrame(incident_weather)
        weather_stats.update({
            'incident_avg_temperature': incident_weather_df['temperature'].mean(),
            'incident_avg_precipitation': incident_weather_df['precipitation'].mean(),
            'incident_avg_wind_speed': incident_weather_df['wind_speed'].mean(),
            'incident_avg_visibility': incident_weather_df['visibility'].mean(),
            'incident_count_with_weather': len(incident_weather)
        })
    
    return weather_stats


def calculate_zone_characteristics(df, cluster_id, weather_df):
    cluster_df = df[df['cluster'] == cluster_id]
    
    if len(cluster_df) == 0:
        return None
    

    center_lat = cluster_df['latitude'].mean()
    center_lng = cluster_df['longitude'].mean()
    

    distances = []
    for _, row in cluster_df.iterrows():
        dist = haversine(center_lat, center_lng, row['latitude'], row['longitude'])
        distances.append(dist)
    
    radius = max(distances) * 1.2 if distances else 500
    

    total_incidents = len(cluster_df)
    avg_fatalities = cluster_df['fatalities'].fillna(0).mean()
    avg_injuries = cluster_df['injuries'].fillna(0).mean()
    

    incident_types = cluster_df['incident_type'].value_counts().to_dict()
    most_common_type = cluster_df['incident_type'].mode()[0] if len(cluster_df) > 0 else 'unknown'
    

    incident_times = cluster_df['created_at'].tolist()
    

    weather_stats = get_weather_for_zone(
        weather_df, 
        center_lat, 
        center_lng, 
        radius,
        incident_times
    )
    

    risk_score = (
        total_incidents * 1.0 +
        avg_fatalities * 10.0 +
        avg_injuries * 3.0
    )
    

    if weather_stats:

        if weather_stats.get('avg_visibility', 10000) < 2000:
            risk_score *= 1.2

        if weather_stats.get('avg_precipitation', 0) > 3:
            risk_score *= 1.15

        if weather_stats.get('avg_wind_speed', 0) > 8:
            risk_score *= 1.1
    

    risk_score = risk_score * (1 + 0.1 * total_incidents)
    
    if risk_score > 100:
        risk_level = 'high'
    elif risk_score > 30:
        risk_level = 'medium'
    else:
        risk_level = 'low'
    

    last_incident = cluster_df['created_at'].max()
    

    type_names = {
        'ДТП': 'аварий',
        'Пожар': 'возгораний',
        'Наезд на пешехода': 'наездов на пешеходов',
        'unknown': 'происшествий'
    }
    type_word = type_names.get(most_common_type, 'происшествий')
    

    if center_lat > 50:
        location_hint = "северный"
    else:
        location_hint = "южный"
    
    return {
        'name': f"Участок частых {type_word} ({location_hint}, {total_incidents} инц.)",
        'center_lat': center_lat,
        'center_lng': center_lng,
        'radius': int(radius),
        'incidents_count': total_incidents,
        'risk_level': risk_level,
        'risk_score': risk_score,
        'avg_fatalities': avg_fatalities,
        'avg_injuries': avg_injuries,
        'most_common_type': most_common_type,
        'weather_stats': weather_stats,
        'last_incident': last_incident
    }


def save_zones_to_db(zones_data):
    engine = get_engine()
    
    if len(zones_data) == 0:
        return
    

    converted_zones = convert_numpy_types(zones_data)
    

    with engine.connect() as conn:
        conn.execute(text("UPDATE dangerous_zones SET is_active = false"))
        conn.commit()
    

    inserted = 0
    updated = 0
    
    for zone in converted_zones:

        check_query = text("""
            SELECT id FROM dangerous_zones 
            WHERE ABS(center_lat - :lat) < 0.01 
              AND ABS(center_lng - :lng) < 0.01
              AND is_active = true
        """)
        
        with engine.connect() as conn:
            existing = conn.execute(
                check_query, 
                {"lat": zone['center_lat'], "lng": zone['center_lng']}
            ).fetchone()
            
            if existing:

                update_query = text("""
                    UPDATE dangerous_zones 
                    SET name = :name,
                        radius = :radius,
                        incidents_count = :incidents_count,
                        risk_level = :risk_level,
                        calculated_at = NOW(),
                        is_active = true
                    WHERE id = :id
                """)
                conn.execute(
                    update_query,
                    {
                        "id": existing[0],
                        "name": zone['name'],
                        "radius": zone['radius'],
                        "incidents_count": zone['incidents_count'],
                        "risk_level": zone['risk_level']
                    }
                )
                updated += 1
            else:

                insert_query = text("""
                    INSERT INTO dangerous_zones 
                        (name, center_lat, center_lng, radius, incidents_count, risk_level, is_active)
                    VALUES 
                        (:name, :lat, :lng, :radius, :incidents_count, :risk_level, true)
                """)
                conn.execute(
                    insert_query,
                    {
                        "name": zone['name'],
                        "lat": zone['center_lat'],
                        "lng": zone['center_lng'],
                        "radius": zone['radius'],
                        "incidents_count": zone['incidents_count'],
                        "risk_level": zone['risk_level']
                    }
                )
                inserted += 1
            conn.commit()
  


def update_dangerous_zones():
    incidents_df = load_incidents()
    weather_df = load_weather()
    
    if len(incidents_df) == 0:
        return
    
    clustered_df, _ = cluster_incidents(incidents_df, eps_meters=3000, min_samples=2)
    

    cluster_ids = clustered_df[clustered_df['cluster'] != -1]['cluster'].unique()
    
    if len(cluster_ids) == 0:
        clustered_df, _ = cluster_incidents(incidents_df, eps_meters=5000, min_samples=2)
        cluster_ids = clustered_df[clustered_df['cluster'] != -1]['cluster'].unique()
    
    if len(cluster_ids) == 0:
        clustered_df, _ = cluster_incidents(incidents_df, eps_meters=10000, min_samples=2)
        cluster_ids = clustered_df[clustered_df['cluster'] != -1]['cluster'].unique()
    

    
    zones_data = []
    for cluster_id in cluster_ids:
        zone_info = calculate_zone_characteristics(clustered_df, cluster_id, weather_df)
        if zone_info:
            zones_data.append(zone_info)
    

    risk_order = {'high': 0, 'medium': 1, 'low': 2}
    zones_data.sort(key=lambda z: risk_order.get(z['risk_level'], 3))

    save_zones_to_db(zones_data)

if __name__ == '__main__':
    update_dangerous_zones()
