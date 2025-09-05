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
const user = tg.initDataUnsafe?.user;
const userId = user?.id;
const initData = tg.initData;

// Отладочная информация
console.log('🔍 Telegram Web App данные:');
console.log('tg.initData:', initData ? 'present (' + initData.length + ' chars)' : 'null');
console.log('tg.initDataUnsafe:', tg.initDataUnsafe);
console.log('user:', user);
console.log('platform:', tg.platform);
console.log('version:', tg.version);

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
    console.log('🔍 Подготовка заголовков аутентификации:');
    console.log('initData:', initData ? 'present (' + initData.length + ' chars)' : 'null');
    console.log('User ID:', user?.id);
    
    const headers = {
        'Content-Type': 'application/json',
        'X-Telegram-Init-Data': initData,
        'X-Telegram-Bot-Name': 'StickerGallery'
    };
    
    console.log('🔍 Заголовки для API запроса:', {
        'Content-Type': headers['Content-Type'],
        'X-Telegram-Init-Data': headers['X-Telegram-Init-Data'] ? 'present' : 'null',
        'X-Telegram-Bot-Name': headers['X-Telegram-Bot-Name']
    });
    
    return headers;
}

// Проверка статуса аутентификации
async function checkAuthStatus() {
    try {
        const authStatusElement = document.getElementById('authStatus');
        authStatusElement.innerHTML = '<p>🔐 Проверка авторизации...</p>';
        authStatusElement.className = 'auth-status';

        // Проверяем наличие initData
        if (!initData || initData.trim() === '') {
            console.error('❌ InitData отсутствует или пустая');
            authStatusElement.innerHTML = `
                <p>❌ Ошибка: InitData не получена от Telegram</p>
                <p>Убедитесь, что приложение открыто через Telegram бота</p>
            `;
            authStatusElement.className = 'auth-status error';
            return false;
        }

        console.log('✅ InitData найдена, отправляем запрос на сервер');
        const response = await fetch(`${AUTH_BASE}/status`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        const authData = await response.json();
        
        if (authData.authenticated) {
            authStatusElement.innerHTML = `
                <p>✅ Авторизация успешна</p>
                <p>Роль: <strong>${authData.role || 'USER'}</strong></p>
            `;
            authStatusElement.className = 'auth-status authenticated';
            return true;
        } else {
            authStatusElement.innerHTML = `
                <p>❌ Ошибка авторизации: ${authData.message}</p>
            `;
            authStatusElement.className = 'auth-status error';
            return false;
        }
    } catch (error) {
        console.error('Ошибка проверки авторизации:', error);
        const authStatusElement = document.getElementById('authStatus');
        authStatusElement.innerHTML = `
            <p>❌ Ошибка проверки авторизации: ${error.message}</p>
        `;
        authStatusElement.className = 'auth-status error';
        return false;
    }
}

// Загрузка стикеров
async function loadStickers() {
    try {
        // Безопасное обновление loading элемента
        const loading = document.getElementById('loading');
        if (loading) {
            loading.innerHTML = '<p>Загрузка стикеров...</p>';
        }

        // Сначала проверяем авторизацию
        const isAuthenticated = await checkAuthStatus();
        if (!isAuthenticated) {
            throw new Error('Пользователь не авторизован');
        }

        const response = await fetch(API_BASE, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            const stickers = await response.json();
            displayStickers(stickers);
        } else if (response.status === 401) {
            throw new Error('Требуется авторизация');
        } else if (response.status === 403) {
            throw new Error('Доступ запрещен');
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

    const stickersHtml = stickers.map(sticker => `
        <div class="sticker-card" data-title="${sticker.title.toLowerCase()}">
            <h3>${sticker.title}</h3>
            <p>ID: ${sticker.id}</p>
            <p>Пользователь: ${sticker.userId}</p>
            <p>Создан: ${new Date(sticker.createdAt).toLocaleDateString()}</p>
            <div class="sticker-actions">
                <button class="btn btn-primary" onclick="openStickerSet('${sticker.name}')">
                    Открыть
                </button>
                <button class="btn btn-secondary" onclick="shareStickerSet('${sticker.name}', '${sticker.title}')">
                    Поделиться
                </button>
                <button class="btn btn-danger" onclick="deleteStickerSet(${sticker.id}, '${sticker.title}')">
                    Удалить
                </button>
            </div>
        </div>
    `).join('');

    content.innerHTML = `
        <div class="sticker-grid" id="stickerGrid">
            ${stickersHtml}
        </div>
    `;
}

// Фильтрация стикеров
function filterStickers() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
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

// Открытие набора стикеров в Telegram
function openStickerSet(stickerSetName) {
    const url = `https://t.me/addstickers/${stickerSetName}`;
    tg.openTelegramLink(url);
}

// Поделиться набором стикеров
function shareStickerSet(stickerSetName, title) {
    const shareText = `🎨 Посмотрите мой набор стикеров "${title}": https://t.me/addstickers/${stickerSetName}`;
    tg.shareUrl(shareText, `https://t.me/addstickers/${stickerSetName}`);
}

// Удаление набора стикеров
async function deleteStickerSet(id, title) {
    if (!confirm(`Вы уверены, что хотите удалить набор стикеров "${title}"?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (response.ok) {
            tg.showAlert(`Набор стикеров "${title}" успешно удален`);
            loadStickers(); // Перезагружаем список
        } else {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
    } catch (error) {
        console.error('Ошибка удаления:', error);
        tg.showAlert(`Ошибка удаления: ${error.message}`);
    }
}

// Функция для отображения отладочной информации
function showDebugInfo() {
    const debugInfo = document.getElementById('debugInfo');
    const debugContent = document.getElementById('debugContent');
    
    const debugData = {
        user: user,
        userId: userId,
        initData: initData,
        initDataLength: initData ? initData.length : 0,
        themeParams: tg.themeParams,
        platform: tg.platform,
        version: tg.version,
        colorScheme: tg.colorScheme
    };
    
    debugContent.textContent = JSON.stringify(debugData, null, 2);
    debugInfo.style.display = 'block';
}

// Обработка кнопки "Назад"
tg.BackButton.onClick(() => {
    tg.close();
});

// Показываем кнопку "Назад"
tg.BackButton.show();

// Добавляем кнопку для отладочной информации (только в dev режиме)
if (tg.initDataUnsafe?.query_id) {
    const debugButton = document.createElement('button');
    debugButton.textContent = '🐛 Debug';
    debugButton.className = 'btn btn-secondary';
    debugButton.style.position = 'fixed';
    debugButton.style.bottom = '20px';
    debugButton.style.right = '20px';
    debugButton.style.zIndex = '1000';
    debugButton.onclick = showDebugInfo;
    document.body.appendChild(debugButton);
}

// Загружаем стикеры при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    console.log('Telegram Web App инициализирован');
    console.log('User:', user);
    console.log('InitData length:', initData ? initData.length : 0);
    loadStickers();
});

// Обработка ошибок
window.addEventListener('error', (event) => {
    console.error('Ошибка:', event.error);
    tg.showAlert('Произошла ошибка в приложении');
});