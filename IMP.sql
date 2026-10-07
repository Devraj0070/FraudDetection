USE fraud_detection;

SET SQL_SAFE_UPDATES = 0;

UPDATE users
SET role = 'ADMIN'
WHERE username = 'testuser3';

SET SQL_SAFE_UPDATES = 1;

SELECT
    id,
    username,
    email,
    role
FROM users
ORDER BY id;