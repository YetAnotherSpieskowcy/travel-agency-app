/*
Database for all of the project's tables (which are service-specific).
*/
SELECT 'CREATE DATABASE rsww_184529'
WHERE
    NOT EXISTS (
        SELECT FROM pg_database
        WHERE datname = 'rsww_184529'
    )
\gexec
