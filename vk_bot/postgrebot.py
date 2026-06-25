import vk_api
from vk_api.longpoll import VkLongPoll, VkEventType
from vk_api.keyboard import VkKeyboard, VkKeyboardColor
import psycopg2
import datetime
import os
import random
import string
import time
import re
from datetime import datetime as dt

DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': os.getenv('DB_PORT', '5433'),
    'dbname': os.getenv('DB_NAME', 'roadcheck'),
    'user': os.getenv('DB_USER', 'roadcheck'),
    'password': os.getenv('DB_PASSWORD', 'roadcheck'),
}

GROUP_TOKEN = os.getenv('VK_GROUP_TOKEN')

ACCIDENT_TYPES = [
    'ДТП',
    'Затор/Пробка',
    'Яма на дороге',
    'Дорожные работы',
    'Плохие погодные условия',
    'Неисправный светофор',
]

def get_db_connection():
    try:
        return psycopg2.connect(**DB_CONFIG)
    except Exception as e:
        print(f"Ошибка подключения к БД: {e}")
        return None

def generate_appeal_id():
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))

def get_or_create_user(user_id, phone=None, firstname=None, lastname=None):
    conn = get_db_connection()
    if not conn:
        return None
    
    cursor = conn.cursor()
    
    cursor.execute("SELECT id FROM public.users WHERE telegram_name = %s", (str(user_id),))
    user = cursor.fetchone()
    
    if user:
        if phone:
            cursor.execute("UPDATE public.users SET phone = %s, updated_at = %s WHERE id = %s", (phone, dt.now(), user[0]))
            conn.commit()
        conn.close()
        return user[0]
    
    cursor.execute(
        "INSERT INTO public.users (telegram_name, phone, firstname, lastname, created_at, updated_at) VALUES (%s, %s, %s, %s, %s, %s) RETURNING id",
        (str(user_id), phone, firstname, lastname, dt.now(), dt.now())
    )
    user_db_id = cursor.fetchone()[0]
    conn.commit()
    conn.close()
    return user_db_id

def format_address_parts(data):
    parts = []
    
    if data.get('region'):
        parts.append(f"Регион: {data['region']}")
    if data.get('city'):
        parts.append(f"Город: {data['city']}")
    if data.get('district'):
        parts.append(f"Район: {data['district']}")
    if data.get('street'):
        parts.append(f"Улица: {data['street']}")
    if data.get('location_desc'):
        parts.append(f"Ориентир: {data['location_desc']}")
    
    if data.get('participants'):
        parts.append(f"Участников: {data['participants']}")
    if data.get('vehicles'):
        parts.append(f"ТС: {data['vehicles']}")
    if data.get('collision_type'):
        parts.append(f"Тип: {data['collision_type']}")
    
    return "\n".join(parts) if parts else data.get('raw_location', 'Не указано')

def save_appeal(user_id, data):
    conn = get_db_connection()
    if not conn:
        send_message(user_id, "Ошибка подключения к базе данных. Попробуйте позже.", get_main_keyboard())
        return False
    
    cursor = conn.cursor()
    
    user_db_id = get_or_create_user(user_id, data.get('phone'), None, None)
    if not user_db_id:
        send_message(user_id, "Ошибка при создании пользователя.", get_main_keyboard())
        conn.close()
        return False
    
    appeal_id = generate_appeal_id()
    now = dt.now()
    
    formatted_address = format_address_parts(data)
    
    cursor.execute(
        """INSERT INTO public.reports 
           (user_id, police_user_id, incident_type, description, address, 
            status, created_at, updated_at, title, latitude, longitude,
            fatalities, injuries, cause)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (
            user_db_id,
            1,
            data['accident_type'],
            data['description'],
            formatted_address,
            'NEW',
            now,
            now,
            f"Обращение {appeal_id}",
            data.get('latitude'),
            data.get('longitude'),
            data.get('fatalities', 0),
            data.get('injuries', 0),
            data.get('cause', 'Не указана')
        )
    )
    
    conn.commit()
    conn.close()
    
    coords_text = ""
    if data.get('latitude') and data.get('longitude'):
        coords_text = f"\nКоординаты: {data['latitude']}, {data['longitude']}"
    
    victims_text = ""
    if data.get('fatalities', 0) > 0 or data.get('injuries', 0) > 0:
        victims_text = f"\nПогибло: {data.get('fatalities', 0)}\nРанено: {data.get('injuries', 0)}"
    
    cause_text = ""
    if data.get('cause'):
        cause_text = f"\nПричина: {data['cause']}"
    
    send_message(
        user_id,
        f"Обращение зарегистрировано\n\n"
        f"Номер: {appeal_id}\n"
        f"Статус: Новое\n"
        f"Дата: {now.strftime('%Y-%m-%d %H:%M:%S')}\n"
        f"Тип: {data['accident_type']}\n"
        f"Место:\n{formatted_address}{coords_text}{victims_text}{cause_text}",
        get_main_keyboard()
    )
    return True

def check_status(user_id, appeal_id):
    conn = get_db_connection()
    if not conn:
        send_message(user_id, "Ошибка подключения к базе данных.", get_main_keyboard())
        return
    
    cursor = conn.cursor()
    
    cursor.execute(
        """SELECT incident_type, description, address, status, created_at, 
                  latitude, longitude, fatalities, injuries, cause
           FROM public.reports WHERE title LIKE %s""",
        (f"%{appeal_id}%",)
    )
    report = cursor.fetchone()
    conn.close()
    
    if report:
        coords_text = f"\nКоординаты: {report[5]}, {report[6]}" if report[5] and report[6] else ""
        victims_text = f"\nПогибло: {report[7]}\nРанено: {report[8]}" if report[7] or report[8] else ""
        cause_text = f"\nПричина: {report[9]}" if report[9] else ""
        
        text = (
            f"Обращение {appeal_id}\n\n"
            f"Тип: {report[0]}\n"
            f"Описание: {report[1]}\n"
            f"Место:\n{report[2]}{coords_text}{victims_text}{cause_text}\n"
            f"Статус: {report[3]}\n"
            f"Создано: {report[4].strftime('%Y-%m-%d %H:%M:%S')}"
        )
    else:
        text = "Обращение не найдено"
    
    send_message(user_id, text, get_main_keyboard())

def parse_location_input(text):
    result = {}
    result['raw_location'] = text
    
    region_pattern = r'(СФО|ДФО|ЦФО|СЗФО|ЮФО|СКФО|ПФО|УрФО)'
    region_match = re.search(region_pattern, text)
    if region_match:
        result['region'] = region_match.group(1)
    
    city_pattern = r'(?:г\.|город)\s*([А-Яа-я\s-]+)'
    city_match = re.search(city_pattern, text)
    if city_match:
        result['city'] = city_match.group(1).strip()
    
    district_pattern = r'(?:р-н|район)\s*([А-Яа-я\s-]+)'
    district_match = re.search(district_pattern, text)
    if district_match:
        result['district'] = district_match.group(1).strip()
    
    street_pattern = r'(?:ул\.|улица|пр\.|проспект|пер\.|переулок)\s*([А-Яа-я\s\d-]+)'
    street_match = re.search(street_pattern, text)
    if street_match:
        result['street'] = street_match.group(1).strip()
    
    participants_pattern = r'участников\s*[:\-]?\s*(\d+)'
    participants_match = re.search(participants_pattern, text, re.IGNORECASE)
    if participants_match:
        result['participants'] = participants_match.group(1)
    
    vehicles_pattern = r'ТС\s*[:\-]?\s*(\d+)'
    vehicles_match = re.search(vehicles_pattern, text, re.IGNORECASE)
    if vehicles_match:
        result['vehicles'] = vehicles_match.group(1)
    
    collision_pattern = r'Тип\s*[:\-]?\s*([А-Яа-я\s]+)'
    collision_match = re.search(collision_pattern, text, re.IGNORECASE)
    if collision_match:
        result['collision_type'] = collision_match.group(1).strip()
    
    return result

def get_main_keyboard():
    keyboard = VkKeyboard(one_time=False)
    keyboard.add_button('Подать обращение', color=VkKeyboardColor.POSITIVE)
    keyboard.add_line()
    keyboard.add_button('Статус обращения', color=VkKeyboardColor.PRIMARY)
    return keyboard

def get_accident_types_keyboard():
    keyboard = VkKeyboard(one_time=True)
    keyboard.add_button(ACCIDENT_TYPES[0], color=VkKeyboardColor.PRIMARY)
    keyboard.add_button(ACCIDENT_TYPES[1], color=VkKeyboardColor.PRIMARY)
    keyboard.add_line()
    keyboard.add_button(ACCIDENT_TYPES[2], color=VkKeyboardColor.PRIMARY)
    keyboard.add_button(ACCIDENT_TYPES[3], color=VkKeyboardColor.PRIMARY)
    keyboard.add_line()
    keyboard.add_button(ACCIDENT_TYPES[4], color=VkKeyboardColor.PRIMARY)
    keyboard.add_button(ACCIDENT_TYPES[5], color=VkKeyboardColor.PRIMARY)
    keyboard.add_line()
    keyboard.add_button('Назад', color=VkKeyboardColor.NEGATIVE)
    return keyboard

def get_yes_no_keyboard():
    keyboard = VkKeyboard(one_time=True)
    keyboard.add_button('Да', color=VkKeyboardColor.POSITIVE)
    keyboard.add_button('Нет', color=VkKeyboardColor.NEGATIVE)
    return keyboard

def send_message(user_id, message, keyboard=None):
    params = {
        'user_id': user_id,
        'message': message,
        'random_id': int(time.time())
    }
    if keyboard:
        params['keyboard'] = keyboard.get_keyboard()
    
    try:
        vk.messages.send(**params)
    except Exception as e:
        print(f"Ошибка отправки сообщения: {e}")

def validate_phone(phone):
    phone = phone.strip()
    if phone.startswith('+7') and len(phone) == 12 and phone[1:].isdigit():
        return True
    if phone.startswith('8') and len(phone) == 11 and phone.isdigit():
        return True
    return False

def validate_datetime(dt_str):
    try:
        datetime.datetime.strptime(dt_str, "%d.%m.%Y %H:%M")
        return True
    except ValueError:
        return False

def validate_number(value, min_val=0, max_val=100):
    try:
        num = int(value)
        return min_val <= num <= max_val
    except ValueError:
        return False

def init_vk():
    if not GROUP_TOKEN:
        print("Не задана переменная окружения VK_GROUP_TOKEN")
        return None, None, None

    try:
        vk_session = vk_api.VkApi(token=GROUP_TOKEN)
        vk = vk_session.get_api()
        longpoll = VkLongPoll(vk_session)
        print("Подключение успешно")
        return vk_session, vk, longpoll
    except Exception as e:
        print(f"Ошибка подключения: {e}")
        print("Проверьте токен и интернет соединение")
        return None, None, None

def run_bot():
    global vk_session, vk, longpoll

    print("Запуск бота...")

    vk_session, vk, longpoll = init_vk()
    if not vk or not longpoll:
        print("Не удалось подключиться к VK. Бот остановлен.")
        raise SystemExit(1)

    user_states = {}
    greeted_users = set()

    print("Бот запущен и готов к работе")

    while True:
        try:
            for event in longpoll.listen():
                if event.type != VkEventType.MESSAGE_NEW or not event.to_me:
                    continue

                user_id = event.user_id
                text = event.text.strip()
                
                if user_id not in greeted_users:
                    greeted_users.add(user_id)
                    send_message(user_id, "Добро пожаловать в бот по сбору информации о происшествиях на дорогах.\n\nВыберите действие:", get_main_keyboard())
                    continue
                
                if text == 'Подать обращение':
                    user_states[user_id] = {'step': 'phone'}
                    send_message(user_id, "Введите номер телефона (+7XXXXXXXXXX или 8XXXXXXXXXX):", get_main_keyboard())
                    continue
                
                if text == 'Статус обращения':
                    user_states[user_id] = {'step': 'check_status'}
                    send_message(user_id, "Введите номер обращения:", get_main_keyboard())
                    continue
                
                if text == 'Назад':
                    if user_id in user_states:
                        del user_states[user_id]
                    send_message(user_id, "Главное меню:", get_main_keyboard())
                    continue
                
                if user_id in user_states:
                    step = user_states[user_id]['step']
                    
                    if step == 'check_status':
                        check_status(user_id, text)
                        del user_states[user_id]
                    
                    elif step == 'phone':
                        if validate_phone(text):
                            user_states[user_id]['phone'] = text
                            user_states[user_id]['step'] = 'accident_type'
                            send_message(user_id, "Выберите тип происшествия:", get_accident_types_keyboard())
                        else:
                            send_message(user_id, "Неверный формат. Попробуйте ещё раз:")
                    
                    elif step == 'accident_type':
                        if text in ACCIDENT_TYPES:
                            user_states[user_id]['accident_type'] = text
                            
                            if text == 'ДТП':
                                user_states[user_id]['step'] = 'fatalities'
                                send_message(user_id, "Сколько человек погибло? (введите число или 0):", get_main_keyboard())
                            else:
                                user_states[user_id]['step'] = 'datetime'
                                send_message(user_id, "Введите дату и время (ДД.ММ.ГГГГ ЧЧ:ММ):\nПример: 03.04.2026 11:40", get_main_keyboard())
                        else:
                            send_message(user_id, "Выберите тип из кнопок:")
                    
                    elif step == 'fatalities':
                        if validate_number(text, 0, 100):
                            user_states[user_id]['fatalities'] = int(text)
                            user_states[user_id]['step'] = 'injuries'
                            send_message(user_id, "Сколько человек ранено? (введите число или 0):", get_main_keyboard())
                        else:
                            send_message(user_id, "Введите корректное число от 0 до 100:")
                    
                    elif step == 'injuries':
                        if validate_number(text, 0, 100):
                            user_states[user_id]['injuries'] = int(text)
                            user_states[user_id]['step'] = 'cause'
                            send_message(user_id, "Укажите причину ДТП (например: превышение скорости, выезд на встречную, непогода и т.д.):", get_main_keyboard())
                        else:
                            send_message(user_id, "Введите корректное число от 0 до 100:")
                    
                    elif step == 'cause':
                        user_states[user_id]['cause'] = text
                        user_states[user_id]['step'] = 'datetime'
                        send_message(user_id, "Введите дату и время (ДД.ММ.ГГГГ ЧЧ:ММ):\nПример: 03.04.2026 11:40", get_main_keyboard())
                    
                    elif step == 'datetime':
                        if validate_datetime(text):
                            user_states[user_id]['datetime'] = text
                            user_states[user_id]['step'] = 'has_coordinates'
                            send_message(user_id, "Хотите указать координаты места происшествия?", get_yes_no_keyboard())
                        else:
                            send_message(user_id, "Неверный формат. Используйте ДД.ММ.ГГГГ ЧЧ:ММ")
                    
                    elif step == 'has_coordinates':
                        if text == 'Да':
                            user_states[user_id]['step'] = 'latitude'
                            send_message(user_id, "Введите широту (например: 55.030199):", get_main_keyboard())
                        elif text == 'Нет':
                            user_states[user_id]['step'] = 'description'
                            send_message(user_id, "Опишите происшествие:", get_main_keyboard())
                        else:
                            send_message(user_id, "Выберите Да или Нет:", get_yes_no_keyboard())
                    
                    elif step == 'latitude':
                        try:
                            lat = float(text)
                            if -90 <= lat <= 90:
                                user_states[user_id]['latitude'] = lat
                                user_states[user_id]['step'] = 'longitude'
                                send_message(user_id, "Введите долготу (например: 82.920250):", get_main_keyboard())
                            else:
                                send_message(user_id, "Широта должна быть от -90 до 90. Попробуйте ещё раз:")
                        except ValueError:
                            send_message(user_id, "Неверный формат. Введите число (например: 55.030199):")
                    
                    elif step == 'longitude':
                        try:
                            lng = float(text)
                            if -180 <= lng <= 180:
                                user_states[user_id]['longitude'] = lng
                                user_states[user_id]['step'] = 'description'
                                send_message(user_id, "Опишите происшествие:", get_main_keyboard())
                            else:
                                send_message(user_id, "Долгота должна быть от -180 до 180. Попробуйте ещё раз:")
                        except ValueError:
                            send_message(user_id, "Неверный формат. Введите число (например: 82.920250):")
                    
                    elif step == 'description':
                        user_states[user_id]['description'] = text
                        user_states[user_id]['step'] = 'location'
                        send_message(user_id, "Укажите место происшествия.\n\nФормат:\nРегион: СФО\nГород/Населенный пункт: Новосибирск\nРайон: Новосибирский\nУлица: Крылова\nОриентир: у ТЦ Мега\n\nИли просто напишите адрес как есть:", get_main_keyboard())
                    
                    elif step == 'location':
                        parsed_location = parse_location_input(text)
                        parsed_location['raw_location'] = text
                        user_states[user_id].update(parsed_location)
                        save_appeal(user_id, user_states[user_id])
                        del user_states[user_id]
                
                else:
                    send_message(user_id, "Используйте кнопки меню.", get_main_keyboard())
        except KeyboardInterrupt:
            print("\nБот остановлен пользователем")
            break
        except Exception as e:
            print(f"Ошибка в основном цикле: {e}")
            print("Переподключение через 5 секунд...")
            time.sleep(5)
            vk_session, vk, longpoll = init_vk()
            if not vk or not longpoll:
                print("Не удалось переподключиться к VK")
                break


if __name__ == '__main__':
    run_bot()
