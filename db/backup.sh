PGPASSWORD="Musaposte@2026!" pg_dump -h localhost -p 9765 -U musaposte -F c -b -v -f mutuel_poste_backup_$(date +%Y%m%d_%H%M%S)backup mutuel_poste
