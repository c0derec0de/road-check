import os
import pandas as pd
import numpy as np
from sqlalchemy import create_engine, text
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import joblib
from datetime import datetime, timedelta
import xgboost as xgb
import json
from tqdm import tqdm
import sys
import warnings
warnings.filterwarnings('ignore')

DB_HOST = os.getenv('DB_HOST', 'roadcheck-postgres')
DB_PORT = os.getenv('DB_PORT', '5432')
DB_NAME = os.getenv('DB_NAME', 'roadcheck')
DB_USER = os.getenv('DB_USER', 'roadcheck')
DB_PASS = os.getenv('DB_PASS', 'roadcheck')
MODEL_DIR = 'ml_models'
MODEL_VERSION = datetime.now().strftime('%Y%m%d_%H%M%S')

os.makedirs(MODEL_DIR, exist_ok=True)

DEFAULT_WEATHER_FEATURES = {
    'avg_temperature': 0,
    'avg_humidity': 0,
    'avg_wind_speed': 0,
    'avg_precipitation': 0,
    'avg_visibility': 10000,
    'bad_weather_days': 0,
}

PERIOD_WINDOWS = {
    '7d': 7,
    '30d': 30,
    '90d': 90,
    '365d': 365,
}

def get_engine():
    from urllib.parse import quote_plus
    encoded_pass = quote_plus(DB_PASS)
    url = f"postgresql://{DB_USER}:{encoded_pass}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
    return create_engine(url)

def convert_numpy_types(obj):
    if isinstance(obj, (np.integer, np.int64, np.int32)):
        return int(obj)
    if isinstance(obj, (np.floating, np.float64, np.float32)):
        return float(obj)
    if isinstance(obj, np.ndarray):
        return obj.tolist()
    if isinstance(obj, (pd.Timestamp, datetime)):
        return obj.isoformat()
    if isinstance(obj, dict):
        return {key: convert_numpy_types(value) for key, value in obj.items()}
    if isinstance(obj, list):
        return [convert_numpy_types(item) for item in obj]
    if isinstance(obj, tuple):
        return tuple(convert_numpy_types(item) for item in obj)
    return obj

def haversine_vectorized(lat1, lon1, lat2, lon2):
    R = 6371000
    lat1_rad = np.radians(lat1)
    lat2_rad = np.radians(lat2)
    delta_lat = np.radians(lat2 - lat1)
    delta_lon = np.radians(lon2 - lon1)
    a = np.sin(delta_lat/2)**2 + np.cos(lat1_rad) * np.cos(lat2_rad) * np.sin(delta_lon/2)**2
    c = 2 * np.arcsin(np.sqrt(a))
    return R * c

class DataLoader:

    def __init__(self):
        self.engine = get_engine()

    def load_all_data(self, months_back=24):
        data = {}
        data['incidents'] = self._load_incidents(months_back)
        data['weather'] = self._load_weather(months_back)
        data['zones'] = self._load_zones()
        return data

    def _load_incidents(self, months_back):
        query = text(f"""
            SELECT id as report_id, incident_type, latitude, longitude, created_at, fatalities, injuries, cause,
            EXTRACT(DOW FROM created_at) as day_of_week, EXTRACT(HOUR FROM created_at) as hour_of_day, EXTRACT(MONTH FROM created_at) as month
            FROM reports
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND created_at >= NOW() - INTERVAL '{months_back} months'
        """)
        return pd.read_sql(query, self.engine, parse_dates=['created_at'])

    def _load_weather(self, months_back):
        query = text(f"""
            SELECT timestamp, temperature, humidity, wind_speed, precipitation, visibility, latitude, longitude
            FROM weather
            WHERE timestamp >= NOW() - INTERVAL '{months_back} months'
            ORDER BY timestamp
        """)
        return pd.read_sql(query, self.engine, parse_dates=['timestamp'])

    def _load_zones(self):
        query = text("""
            SELECT id as zone_id, name, center_lat, center_lng, radius, risk_level
            FROM dangerous_zones
            WHERE is_active = true
        """)
        return pd.read_sql(query, self.engine)

class FeatureEngineer:

    def __init__(self, data):
        self.data = data

    def create_features_for_zones(self):
        if len(self.data['zones']) == 0:
            return None

        zones, incidents, weather = self.data['zones'], self.data['incidents'], self.data['weather']
        now = datetime.now()
        features_list = []

        for _, zone in tqdm(zones.iterrows(), total=len(zones), desc="Обработка зон"):
            distances = haversine_vectorized(zone['center_lat'], zone['center_lng'], incidents['latitude'].values, incidents['longitude'].values)
            in_zone = distances <= zone['radius'] * 1.5
            zone_incidents = incidents[in_zone].copy()

            if len(zone_incidents) == 0:
                continue

            features = {
                'zone_id': zone['zone_id'], 'latitude': zone['center_lat'], 'longitude': zone['center_lng'],
                'radius': zone['radius'], 'current_risk': zone['risk_level']
            }

            periods = {name: now - timedelta(days=days) for name, days in PERIOD_WINDOWS.items()}
            for period_name, period_start in periods.items():
                period_incidents = zone_incidents[zone_incidents['created_at'] >= period_start]
                features.update(self._calculate_period_features(period_incidents, period_name))

            features.update(self._get_weather_features(zone, zone_incidents, weather))
            features['target_risk'] = self._calculate_target_risk(zone_incidents)
            features_list.append(features)

        return pd.DataFrame(features_list) if features_list else None

    def _calculate_period_features(self, period_incidents, period_name):
        features = {f'{period_name}_count': 0, f'{period_name}_fatalities': 0, f'{period_name}_injuries': 0}
        if len(period_incidents) > 0:
            features.update({
                f'{period_name}_count': len(period_incidents),
                f'{period_name}_fatalities': period_incidents['fatalities'].sum(),
                f'{period_name}_injuries': period_incidents['injuries'].sum(),
            })
        return features

    def _get_weather_features(self, zone, zone_incidents, weather_df):
        if len(weather_df) == 0: return DEFAULT_WEATHER_FEATURES.copy()
        distances = haversine_vectorized(zone['center_lat'], zone['center_lng'], weather_df['latitude'].values, weather_df['longitude'].values)
        nearby_weather = weather_df[distances <= 50000]
        if len(nearby_weather) == 0: return DEFAULT_WEATHER_FEATURES.copy()
        return {
            'avg_temperature': nearby_weather['temperature'].mean(),
            'avg_humidity': nearby_weather['humidity'].mean(),
            'avg_wind_speed': nearby_weather['wind_speed'].mean(),
            'avg_precipitation': nearby_weather['precipitation'].mean(),
            'avg_visibility': nearby_weather['visibility'].mean(),
            'bad_weather_days': len(nearby_weather[nearby_weather['precipitation'] > 5]),
        }

    def _calculate_target_risk(self, zone_incidents):
        recent = zone_incidents[zone_incidents['created_at'] >= datetime.now() - timedelta(days=90)]
        score = len(recent) * 1.0 + recent['fatalities'].sum() * 10 + recent['injuries'].sum() * 3
        return 'high' if score > 50 else 'medium' if score > 20 else 'low'

class RiskModelTrainer:
    def __init__(self):
        self.scaler = StandardScaler()
        self.risk_mapping = {'low': 0, 'medium': 1, 'high': 2}
        self.reverse_mapping = {0: 'low', 1: 'medium', 2: 'high'}

    def train(self, df):
        exclude = ['zone_id', 'latitude', 'longitude', 'current_risk', 'target_risk']
        feature_cols = [col for col in df.columns if col not in exclude and df[col].dtype in ['int64', 'float64']]

        X = self.scaler.fit_transform(df[feature_cols].fillna(0))
        y = df['target_risk'].map(self.risk_mapping)

        num_unique_classes = len(np.unique(y))

        if num_unique_classes < 2:
            print(f"Внимание: Найдено только {num_unique_classes} класс(а). Обучение классификатора требует минимум 2.")
            n_classes = 3
        else:
            n_classes = num_unique_classes

        model = xgb.XGBClassifier(
            n_estimators=100,
            objective='multi:softprob',
            num_class=3,
            random_state=42
        )

        model.fit(X, y)

        model.save_model(os.path.join(MODEL_DIR, f'risk_model_{MODEL_VERSION}.json'))
        joblib.dump(self.scaler, os.path.join(MODEL_DIR, f'scaler_{MODEL_VERSION}.pkl'))
        with open(os.path.join(MODEL_DIR, f'metadata_{MODEL_VERSION}.json'), 'w') as f:
            json.dump({
                'feature_columns': feature_cols,
                'risk_mapping': self.risk_mapping,
                'reverse_mapping': self.reverse_mapping,
                'version': MODEL_VERSION
            }, f)
        return MODEL_VERSION

class RiskPredictor:
    def __init__(self, version):
        with open(os.path.join(MODEL_DIR, f'metadata_{version}.json'), 'r') as f:
            self.metadata = json.load(f)
        self.model = xgb.XGBClassifier()
        self.model.load_model(os.path.join(MODEL_DIR, f'risk_model_{version}.json'))
        self.scaler = joblib.load(os.path.join(MODEL_DIR, f'scaler_{version}.pkl'))
        self.engine = get_engine()
        self.reverse_mapping = {int(k): v for k, v in self.metadata['reverse_mapping'].items()}

    def predict_for_all_zones(self, df):
        predictions = []
        X = self.scaler.transform(df[self.metadata['feature_columns']].fillna(0))
        probs = self.model.predict_proba(X)
        preds = self.model.predict(X)

        for i, row in df.iterrows():
            now = datetime.now()
            predictions.append({
                'zone_id': int(row['zone_id']), 'latitude': float(row['latitude']), 'longitude': float(row['longitude']),
                'risk_level': self.reverse_mapping[int(preds[i])], 'risk_score': round(float(probs[i][int(preds[i])] * 100), 2),
                'probability_low': float(probs[i][0]), 'probability_medium': float(probs[i][1]), 'probability_high': float(probs[i][2]),
                'model_version': self.metadata['version'], 'calculated_at': now, 'is_active': True,
                'region_name': 'Москва', 'city_name': 'Москва'
            })
        return predictions

    def save_predictions_to_db(self, predictions):
        with self.engine.begin() as conn:
            conn.execute(text("UPDATE risk_predictions SET is_active = false"))
            query = text("""
                INSERT INTO risk_predictions (zone_id, latitude, longitude, risk_level, risk_score,
                probability_low, probability_medium, probability_high, model_version, calculated_at, is_active, region_name, city_name)
                VALUES (:zone_id, :latitude, :longitude, :risk_level, :risk_score,
                :probability_low, :probability_medium, :probability_high, :model_version, :calculated_at, :is_active, :region_name, :city_name)
            """)
            for pred in predictions:
                conn.execute(query, convert_numpy_types(pred))

def run_prediction_pipeline(retrain=False):
    loader = DataLoader()
    data = loader.load_all_data()
    engineer = FeatureEngineer(data)
    df = engineer.create_features_for_zones()

    if df is None or df.empty: return

    models = [f for f in os.listdir(MODEL_DIR) if f.startswith('metadata_')]
    if retrain or not models:
        trainer = RiskModelTrainer()
        version = trainer.train(df)
    else:
        version = sorted(models)[-1].replace('metadata_', '').replace('.json', '')

    predictor = RiskPredictor(version)
    preds = predictor.predict_for_all_zones(df)
    predictor.save_predictions_to_db(preds)

if __name__ == '__main__':
    run_prediction_pipeline(retrain='--retrain' in sys.argv)