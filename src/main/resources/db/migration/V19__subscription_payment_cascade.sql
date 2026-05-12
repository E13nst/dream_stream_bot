-- При удалении подписки админом удаляются связанные записи платежей (история checkout).

ALTER TABLE subscription_payment
    DROP CONSTRAINT IF EXISTS subscription_payment_subscription_id_fkey;

ALTER TABLE subscription_payment
    ADD CONSTRAINT subscription_payment_subscription_id_fkey
        FOREIGN KEY (subscription_id) REFERENCES subscription (id) ON DELETE CASCADE;
