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

// Функция для обновления отладочной информации с результатом запроса
function updateDebugInfoWithResponse(status, statusText) {
    const debugContent = document.getElementById('debugContent');
    if (!debugContent) return;
    
    // Добавляем информацию о последнем запросе
    const responseInfo = `
<div class="debug-item" style="border-left-color: ${status === 200 ? '#4CAF50' : '#F44336'};">
    <span class="debug-label">🌐 Последний запрос к API:</span>
    <span class="debug-value">${status} ${statusText}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🕐 Время запроса:</span>
    <span class="debug-value">${new Date().toLocaleTimeString()}</span>
</div>
    `;
    
    // Добавляем в начало отладочной информации
    debugContent.innerHTML = responseInfo + debugContent.innerHTML;
}

// Функция для отображения отладочной информации
function updateDebugInfo() {
    const debugContent = document.getElementById('debugContent');
    if (!debugContent) return;
    
    const now = new Date();
    const authDate = initData ? new URLSearchParams(initData).get('auth_date') : null;
    const authDateTime = authDate ? new Date(parseInt(authDate) * 1000) : null;
    const signature = initData ? new URLSearchParams(initData).get('signature') : null;
    const hash = initData ? new URLSearchParams(initData).get('hash') : null;
    const queryId = initData ? new URLSearchParams(initData).get('query_id') : null;
    
    const debugInfo = `
<div class="debug-item">
    <span class="debug-label">🕐 Текущее время:</span>
    <span class="debug-value">${now.toLocaleString()}</span>
</div>

<div class="debug-item">
    <span class="debug-label">📱 Telegram Platform:</span>
    <span class="debug-value">${tg.platform || 'unknown'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">📋 Telegram Version:</span>
    <span class="debug-value">${tg.version || 'unknown'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🔐 InitData присутствует:</span>
    <span class="debug-value">${initData ? '✅ Да' : '❌ Нет'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">📏 InitData длина:</span>
    <span class="debug-value">${initData ? initData.length + ' символов' : 'N/A'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🕒 Auth Date:</span>
    <span class="debug-value">${authDateTime ? authDateTime.toLocaleString() : 'не найден'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">⏰ Возраст InitData:</span>
    <span class="debug-value">${authDate ? Math.floor((now.getTime() - authDateTime.getTime()) / 1000) + ' секунд' : 'N/A'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">✍️ Signature:</span>
    <span class="debug-value">${signature ? '✅ Присутствует (' + signature.length + ' символов)' : '❌ Отсутствует'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">#️⃣ Hash:</span>
    <span class="debug-value">${hash ? '✅ Присутствует (' + hash.length + ' символов)' : '❌ Отсутствует'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🆔 Query ID:</span>
    <span class="debug-value">${queryId || 'не найден'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">👤 User ID:</span>
    <span class="debug-value">${user?.id || 'не найден'}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🌐 API Endpoint:</span>
    <span class="debug-value">${window.location.origin}/auth/status</span>
</div>

<div class="debug-item">
    <span class="debug-label">✅ InitData валидация:</span>
    <span class="debug-value">${initDataCheck.valid ? '✅ Валидна' : '❌ ' + initDataCheck.reason}</span>
</div>

<div class="debug-item">
    <span class="debug-label">🔤 InitData (первые 100 символов):</span>
    <span class="debug-value">${initData ? initData.substring(0, 100) + '...' : 'отсутствует'}</span>
</div>
    `;
    
    debugContent.innerHTML = debugInfo;
}

// Обновляем отладочную информацию при загрузке
updateDebugInfo();

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
    
    // Проверяем срок действия текущего initData
    const check = checkInitDataExpiry(initData);
    if (!check.valid) {
        console.warn('⚠️ initData невалиден:', check.reason);
        
        // Попытка обновить initData
        if (refreshInitData()) {
            const newCheck = checkInitDataExpiry(initData);
            if (!newCheck.valid) {
                console.error('❌ Не удалось получить валидный initData после обновления');
            }
        }
    }
    
    console.log('initData:', initData ? 'present (' + initData.length + ' chars)' : 'null');
    console.log('User ID:', user?.id);
    
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
            console.warn('⚠️ initData отсутствует, пропускаем проверку аутентификации');
            document.getElementById('authStatus').innerHTML = `
                <div class="auth-error">
                    ❌ Данные аутентификации отсутствуют.
                    <br>Убедитесь, что приложение запущено из Telegram.
                    <br><button onclick="retryWithRefresh()" class="retry-btn">🔄 Попробовать снова</button>
                </div>
            `;
            return false;
        }
        
        // Проверяем срок действия initData
        const check = checkInitDataExpiry(initData);
        if (!check.valid) {
            console.warn('⚠️ initData невалиден:', check.reason);
            document.getElementById('authStatus').innerHTML = `
                <div class="auth-error">
                    ❌ Данные аутентификации устарели.
                    <br>${check.reason}
                    <br><button onclick="retryWithRefresh()" class="retry-btn">🔄 Обновить данные</button>
                </div>
            `;
            
            // Попытка автоматического обновления
            if (refreshInitData()) {
                console.log('✅ initData обновлен, повторяем проверку...');
                return await checkAuthStatus();
            }
            return false;
        }
        
        console.log('🔐 Проверяем статус аутентификации...');
        console.log('📊 initData возраст:', check.age, 'сек из', check.maxAge, 'сек');
        
        const response = await fetch(`${AUTH_BASE}/status`, {
            method: 'GET',
            headers: getAuthHeaders()
        });
        
        console.log('📊 Ответ сервера на проверку аутентификации:', response.status, response.statusText);
        
        // Обновляем отладочную информацию с результатом запроса
        updateDebugInfoWithResponse(response.status, response.statusText);
        
        if (response.ok) {
            const authData = await response.json();
            console.log('✅ Данные аутентификации:', authData);
            
            if (authData.authenticated) {
                document.getElementById('authStatus').innerHTML = `
                    <div class="auth-success">
                        ✅ Аутентификация успешна
                        <br>Роль: ${authData.role || 'не определена'}
                        <br>ID: ${authData.telegramId || 'не определен'}
                        <br><small>Данные действительны ещё ${check.maxAge - check.age} сек</small>
                    </div>
                `;
                return true;
            } else {
                document.getElementById('authStatus').innerHTML = `
                    <div class="auth-error">
                        ❌ Ошибка авторизации: ${authData.message || 'Неизвестная ошибка'}
                        <br><button onclick="retryWithRefresh()" class="retry-btn">🔄 Попробовать снова</button>
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
                    <br><button onclick="retryWithRefresh()" class="retry-btn">🔄 Попробовать снова</button>
                </div>
            `;
            return false;
        }
    } catch (error) {
        console.error('❌ Ошибка при проверке аутентификации:', error);
        document.getElementById('authStatus').innerHTML = `
            <div class="auth-error">
                ❌ Ошибка сети: ${error.message}
                <br><button onclick="retryWithRefresh()" class="retry-btn">🔄 Попробовать снова</button>
            </div>
        `;
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