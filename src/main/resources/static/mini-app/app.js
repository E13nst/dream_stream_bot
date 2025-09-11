// Инициализация Telegram Web App
const tg = window.Telegram.WebApp;
tg.expand();
tg.ready();

// Настройка темы
document.documentElement.style.setProperty('--tg-theme-bg-color', tg.themeParams.bg_color || '#ffffff');
document.documentElement.style.setProperty('--tg-theme-text-color', tg.themeParams.text_color || '#000000');
document.documentElement.style.setProperty('--tg-theme-hint-color', tg.themeParams.hint_color || '#999999');
document.documentElement.style.setProperty('--tg-theme-button-color', tg.themeParams.button_color || '#2481cc');
document.documentElement.style.setProperty('--tg-theme-button-text-color', tg.themeParams.button_text_color || '#ffffff');
document.documentElement.style.setProperty('--tg-theme-secondary-bg-color', tg.themeParams.secondary_bg_color || '#f8f9fa');

// Получение информации о пользователе
let user = tg.initDataUnsafe?.user;
const userId = user?.id;
let initData = tg.initData;

// Отладочная информация
console.log('🔍 Telegram Web App данные:');
console.log('tg.initData:', initData ? 'present (' + initData.length + ' chars)' : 'null');
console.log('tg.initDataUnsafe:', tg.initDataUnsafe);
console.log('user:', user);
console.log('platform:', tg.platform);
console.log('version:', tg.version);

// Функция проверки срока действия initData
function checkInitDataExpiry(initDataString) {
    if (!initDataString) return { valid: false, reason: 'initData отсутствует' };
    
    try {
        const params = new URLSearchParams(initDataString);
        const authDate = parseInt(params.get('auth_date'));
        
        if (!authDate) {
            return { valid: false, reason: 'auth_date отсутствует' };
        }
        
        const now = Math.floor(Date.now() / 1000);
        const age = now - authDate;
        const maxAge = 600; // 10 минут (как в валидаторе)
        
        console.log('🕐 Проверка срока действия initData:');
        console.log('auth_date:', authDate, '(' + new Date(authDate * 1000).toLocaleString() + ')');
        console.log('current time:', now, '(' + new Date(now * 1000).toLocaleString() + ')');
        console.log('age:', age, 'секунд');
        console.log('max age:', maxAge, 'секунд');
        
        if (age > maxAge) {
            return { 
                valid: false, 
                reason: `initData устарел (возраст: ${age} сек, максимум: ${maxAge} сек)`,
                age: age,
                maxAge: maxAge
            };
        }
        
        return { valid: true, age: age, maxAge: maxAge };
    } catch (error) {
        console.error('❌ Ошибка при проверке срока действия initData:', error);
        return { valid: false, reason: 'Ошибка парсинга initData: ' + error.message };
    }
}

// Функция обновления initData
function refreshInitData() {
    console.log('🔄 Попытка обновления initData...');
    
    // Перезагружаем данные из Telegram Web App
    const newUser = tg.initDataUnsafe?.user;
    const newInitData = tg.initData;
    
    if (newInitData && newInitData !== initData) {
        console.log('✅ initData обновлен');
        user = newUser;
        initData = newInitData;
        return true;
    } else {
        console.log('❌ initData не изменился');
        return false;
    }
}

// Функция повторной попытки с обновлением
function retryWithRefresh() {
    console.log('🔄 Повторная попытка с обновлением данных...');
    if (refreshInitData()) {
        console.log('✅ Данные обновлены, перезагружаем стикеры...');
        loadStickers();
    } else {
        console.log('❌ Не удалось обновить данные, попробуйте перезапустить приложение из бота');
        alert('Не удалось обновить данные аутентификации. Попробуйте перезапустить приложение из бота.');
    }
}

// Проверяем срок действия initData при загрузке
const initDataCheck = checkInitDataExpiry(initData);
if (!initDataCheck.valid) {
    console.warn('⚠️ Проблема с initData:', initDataCheck.reason);
}

// Функция для логирования отладочной информации с результатом запроса
function logDebugInfoWithResponse(status, statusText) {
    console.log('🌐 Последний запрос к API:', status, statusText);
    console.log('🕐 Время запроса:', new Date().toLocaleTimeString());
}

// Отображение информации о пользователе
if (user) {
    document.getElementById('userInfo').innerHTML = `
        <p>Привет, <strong>${user.first_name}${user.last_name ? ' ' + user.last_name : ''}</strong>!</p>
        <p>ID: <strong>${user.id}</strong></p>
        ${user.username ? `<p>Username: <strong>@${user.username}</strong></p>` : ''}
    `;
} else {
    document.getElementById('userInfo').innerHTML = `
        <p>Пользователь не определен</p>
    `;
}

// API базовый URL
const API_BASE = '/api/stickersets';
const AUTH_BASE = '/auth';

// Функция для добавления заголовков аутентификации
function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };
    
    if (initData) {
        headers['X-Telegram-Init-Data'] = initData;
        headers['X-Telegram-Bot-Name'] = 'StickerGallery';
        console.log('✅ Заголовки аутентификации добавлены');
        console.log('X-Telegram-Bot-Name: StickerGallery');
        console.log('X-Telegram-Init-Data: present');
    } else {
        console.warn('⚠️ initData отсутствует, запрос без аутентификации');
    }
    
    return headers;
}

// Проверка статуса аутентификации
async function checkAuthStatus() {
    try {
        if (!initData || initData.trim() === '') {
            document.getElementById('authStatus').innerHTML = `
                <div class="auth-error">
                    ❌ Данные аутентификации отсутствуют.
                    <br>Убедитесь, что приложение запущено из Telegram.
                </div>
            `;
            return false;
        }
        
        const response = await fetch('/auth/status', {
            method: 'GET',
            headers: getAuthHeaders()
        });
        
        // Логируем результат запроса
        logDebugInfoWithResponse(response.status, response.statusText);
        
        if (response.ok) {
            const authData = await response.json();
            console.log('✅ Данные аутентификации:', authData);
            
            if (authData.authenticated) {
                document.getElementById('authStatus').innerHTML = `
                    <div class="auth-success">
                        ✅ Аутентификация успешна
                        <br>Роль: ${authData.role || 'не определена'}
                    </div>
                `;
                return true;
            } else {
                document.getElementById('authStatus').innerHTML = `
                    <div class="auth-error">
                        ❌ Ошибка авторизации: ${authData.message || 'Неизвестная ошибка'}
                    </div>
                `;
                return false;
            }
        } else {
            const errorText = await response.text();
            console.error('❌ Ошибка проверки аутентификации:', response.status, errorText);
            document.getElementById('authStatus').innerHTML = `
                <div class="auth-error">
                    ❌ Ошибка сервера: ${response.status} ${response.statusText}
                </div>
            `;
            return false;
        }
    } catch (error) {
        document.getElementById('authStatus').innerHTML = `
            <div class="auth-error">
                ❌ Ошибка сети: ${error.message}
            </div>
        `;
        return false;
    }
}

// Загрузка стикеров
async function loadStickers() {
    try {
        const loading = document.getElementById('loading');
        if (loading) {
            loading.innerHTML = '<p>Загрузка стикеров...</p>';
        }

        // Сначала проверяем авторизацию
        const isAuthenticated = await checkAuthStatus();
        if (!isAuthenticated) {
            throw new Error('Пользователь не авторизован');
        }

        const response = await fetch('/api/stickersets', {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const stickers = await response.json();
            displayStickers(stickers);
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка загрузки стикеров:', error);
        document.getElementById('content').innerHTML = `
            <div class="error">
                <p>Ошибка загрузки стикеров: ${error.message}</p>
                <button class="btn btn-primary" onclick="loadStickers()">Повторить</button>
            </div>
        `;
    }
}

// Отображение стикеров
function displayStickers(stickers) {
    const content = document.getElementById('content');
    
    if (!stickers || stickers.length === 0) {
        content.innerHTML = `
            <div class="empty-state">
                <h3>🎨 Стикеры не найдены</h3>
                <p>У вас пока нет созданных наборов стикеров</p>
                <button class="btn btn-primary" onclick="tg.openTelegramLink('https://t.me/StickerGalleryBot')">
                    Создать стикер
                </button>
            </div>
        `;
        return;
    }

    content.innerHTML = '';

    const stickersHtml = stickers.map(sticker => `
        <div class="sticker-card" data-title="${sticker.title.toLowerCase()}">
            <h3>${sticker.title}</h3>
            <p>ID: ${sticker.id}</p>
            <p>Пользователь: ${sticker.userId}</p>
            <p>Создан: ${new Date(sticker.createdAt).toLocaleDateString()}</p>
            <div class="sticker-actions">
                <button class="btn btn-primary" onclick="openStickerSet('${sticker.title}')">
                    Открыть
                </button>
                <button class="btn btn-secondary" onclick="shareStickerSet('${sticker.title}', '${sticker.title}')">
                    Поделиться
                </button>
                <button class="btn btn-danger" onclick="deleteStickerSet('${sticker.id}', '${sticker.title}')">
                    Удалить
                </button>
            </div>
        </div>
    `).join('');

    content.innerHTML = stickersHtml;
}

// Фильтрация стикеров
function filterStickers() {
    const searchInput = document.getElementById('searchInput');
    const searchTerm = searchInput.value.toLowerCase();
    const stickerCards = document.querySelectorAll('.sticker-card');

    stickerCards.forEach(card => {
        const title = card.getAttribute('data-title');
        if (title.includes(searchTerm)) {
            card.style.display = 'block';
        } else {
            card.style.display = 'none';
        }
    });
}

// Открытие набора стикеров
function openStickerSet(stickerSetName) {
    tg.openTelegramLink(`https://t.me/addstickers/${stickerSetName}`);
}

// Поделиться набором стикеров
function shareStickerSet(stickerSetName, title) {
    tg.openTelegramLink(`https://t.me/addstickers/${stickerSetName}`);
}

// Удаление набора стикеров
async function deleteStickerSet(id, title) {
    if (!confirm(`Вы уверены, что хотите удалить набор стикеров "${title}"?`)) {
        return;
    }

    try {
        const response = await fetch(`/api/stickersets/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            loadStickers(); // Перезагружаем список
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка удаления стикера:', error);
        alert(`Ошибка удаления стикера: ${error.message}`);
    }
}

// Обработка кнопки "Назад"
tg.BackButton.onClick(() => {
    tg.close();
});

// Показываем кнопку "Назад"
tg.BackButton.show();

// Отображение информации о пользователе
if (user) {
    document.getElementById('userInfo').innerHTML = `
        <p>Привет, <strong>${user.first_name}${user.last_name ? ' ' + user.last_name : ''}</strong>!</p>
        <p>ID: <strong>${user.id}</strong></p>
        ${user.username ? `<p>Username: <strong>@${user.username}</strong></p>` : ''}
    `;
} else {
    document.getElementById('userInfo').innerHTML = `
        <p>Пользователь не определен</p>
    `;
}

// Загружаем стикеры при загрузке страницы
document.addEventListener('DOMContentLoaded', loadStickers);

// Обработка ошибок
window.addEventListener('error', (event) => {
    console.error('Ошибка:', event.error);
});