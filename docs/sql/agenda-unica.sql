-- SC-03: un especialista no puede tener dos citas activas en el mismo horario.
--
-- La validación en Java da el mensaje amable, pero entre la comprobación y el INSERT
-- hay una ventana en la que dos peticiones simultáneas pasan las dos. Solo la base de
-- datos puede garantizarlo, y este script es esa garantía.
--
-- Una cita CANCELADA libera el horario: la columna generada vale NULL cuando la cita
-- está cancelada, y en un índice único los NULL no colisionan entre sí.

-- 1. Revisa duplicados ANTES de crear el índice. Si esta consulta devuelve filas, el
--    ALTER del paso 2 fallará: hay que decidir qué cita se conserva en cada grupo.
SELECT doctor_id, fecha, hora, COUNT(*) AS n,
       GROUP_CONCAT(id ORDER BY id) AS ids,
       GROUP_CONCAT(estado ORDER BY id) AS estados
FROM cita
WHERE doctor_id IS NOT NULL AND (estado IS NULL OR estado <> 'CANCELADA')
GROUP BY doctor_id, fecha, hora
HAVING COUNT(*) > 1;

-- 2. Columna generada + índice único (requiere MySQL 8).
ALTER TABLE cita
  ADD COLUMN cupo_activo TINYINT
    GENERATED ALWAYS AS (CASE WHEN estado IS NULL OR estado = 'CANCELADA' THEN NULL ELSE 1 END) STORED;

ALTER TABLE cita
  ADD UNIQUE KEY uk_cita_doctor_horario (doctor_id, fecha, hora, cupo_activo);

-- 3. Para revertirlo:
-- ALTER TABLE cita DROP INDEX uk_cita_doctor_horario, DROP COLUMN cupo_activo;
