#!/usr/bin/env python3
import redis
import os

# Загружаем переменные окружения из .env.app
def load_env():
    env_vars = {}
    try:
        with open('.env.app', 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    env_vars[key] = value
    except FileNotFoundError:
        print("Файл .env.app не найден")
        return {}
    return env_vars

# Загружаем настройки
env = load_env()

# Подключение к Redis
try:
    r = redis.Redis(
        host=env.get('REDIS_HOST', 'localhost'),
        port=int(env.get('REDIS_PORT', 6379)),
        password=env.get('REDIS_PASSWORD', ''),
        ssl=True,  # Включаем SSL
        ssl_cert_reqs=None,  # Отключаем проверку сертификата
        decode_responses=True
    )
    
    print(f"Подключаемся к Redis: {env.get('REDIS_HOST')}:{env.get('REDIS_PORT')}")
    
    # Проверка подключения
    response = r.ping()
    if response:
        print("✅ Подключение к Redis успешно!")
        
        # Дополнительные тесты
        print("\n📊 Тестируем базовые операции:")
        
        # Тест записи
        r.set('test_key', 'test_value', ex=60)  # TTL 60 секунд
        print("✅ Запись в Redis: OK")
        
        # Тест чтения
        value = r.get('test_key')
        print(f"✅ Чтение из Redis: {value}")
        
        # Тест удаления
        r.delete('test_key')
        print("✅ Удаление из Redis: OK")
        
        # Информация о Redis
        info = r.info()
        print(f"\n📈 Информация о Redis:")
        print(f"   Версия: {info.get('redis_version', 'неизвестно')}")
        print(f"   Использованная память: {info.get('used_memory_human', 'неизвестно')}")
        print(f"   Подключенные клиенты: {info.get('connected_clients', 'неизвестно')}")
        
    else:
        print("❌ Не удалось подключиться к Redis.")
        
except redis.ConnectionError as e:
    print(f"❌ Ошибка подключения к Redis: {e}")
except redis.AuthenticationError as e:
    print(f"❌ Ошибка аутентификации Redis: {e}")
except Exception as e:
    print(f"❌ Произошла ошибка: {e}")
