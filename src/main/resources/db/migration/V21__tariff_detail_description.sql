-- Подробное описание тарифа для экрана перед оплатой в боте.
ALTER TABLE subscription_tariff ADD COLUMN IF NOT EXISTS detail_description TEXT;

-- Осмысленные тексты для групповых оплачиваемых тарифов.
UPDATE subscription_tariff
SET detail_description = E'• Доступ для группового чата в рамках лимита участников по тарифу\n• После успешной оплаты доступ продлевается на указанный срок\n• Управление и оплата доступны администратору группы в боте'
WHERE scope = 'GROUP'
  AND access_mode = 'PAID_TERM'
  AND (detail_description IS NULL OR btrim(detail_description) = '');

-- Если у персональных PAID уже задана цена — универсальный текст.
UPDATE subscription_tariff
SET detail_description = E'• Полный доступ к материалам на период подписки\n• После успешной оплаты доступ активируется автоматически (или воспользуйтесь «Я оплатил», если оповещение задерживается)'
WHERE scope = 'PERSONAL'
  AND access_mode = 'PAID_TERM'
  AND price_amount_minor IS NOT NULL
  AND (detail_description IS NULL OR btrim(detail_description) = '');
