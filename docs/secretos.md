# Secretos del proyecto (SC-01)

## Qué había expuesto

`application.properties` y `JwtUtil.java` llevaban los valores reales escritos en el
código, así que quedaron en el historial de Git desde el primer commit:

| Secreto | Dónde estaba | Estado |
|---|---|---|
| Contraseña de MySQL (`root`) | `spring.datasource.password` | ya fuera del archivo, **sin rotar** |
| App password de Gmail | `spring.mail.password` | ya fuera del archivo, **sin rotar** |
| Clave de firma JWT | `JwtUtil.SECRET` | ya fuera del código, **sin rotar** |

## Qué se hizo

- Los valores viven ahora en `.env`, que está en `.gitignore` y no se versiona.
- `application.properties` los carga con `spring.config.import=optional:file:./.env[.properties]`
  y los referencia sin valor por defecto (`${DB_PASSWORD}`, `${MAIL_PASSWORD}`, `${JWT_SECRET}`).
- `JwtUtil` recibe la clave por constructor con `@Value("${JWT_SECRET}")`.
- **Sin secretos la aplicación no arranca.** Es intencional: antes arrancaba con la
  credencial del repositorio y eso es peor que un error al inicio.

`.env.example` documenta las claves necesarias sin valores.

## Qué falta y solo puede hacer el dueño del proyecto

Sacar los valores del archivo **no** los borra del historial: cualquiera con acceso al
repositorio puede leer los commits anteriores. Por eso hay que **rotar**:

1. **MySQL**. Cambiar la contraseña del usuario que usa la app:
   ```sql
   ALTER USER 'root'@'localhost' IDENTIFIED BY 'NUEVA_CONTRASEÑA';
   FLUSH PRIVILEGES;
   ```
   Después actualizar `DB_PASSWORD` en `.env` (y en cualquier otra máquina o IDE que
   se conecte a esa base).

2. **Gmail**. Entrar a la cuenta `tuconsultoriopsiquiatrico@gmail.com` →
   Seguridad → Contraseñas de aplicaciones: **revocar** la actual y crear una nueva.
   Actualizar `MAIL_PASSWORD` en `.env`. Revocar es lo que invalida la que está en el
   historial; crear otra sin revocar no sirve.

3. **JWT**. Generar una clave nueva (mínimo 32 bytes):
   ```bash
   openssl rand -base64 48
   ```
   Ponerla en `JWT_SECRET`. Al cambiarla **caducan todas las sesiones abiertas**: cada
   usuario tendrá que volver a entrar. Conviene hacerlo fuera del horario de consulta.

4. Opcional: reescribir el historial (`git filter-repo`) para borrar los valores
   antiguos. Es destructivo, obliga a `--force` y a que todos vuelvan a clonar; con los
   secretos ya rotados no aporta gran cosa.

## Cómo comprobar que quedó bien

```bash
git grep -nE "brony|xqnt|mi_clave_secreta" -- src   # no debe devolver nada
```

Y que la app sigue arrancando con su `.env`:

```bash
./mvnw spring-boot:run
```

Si falta una clave, el arranque falla con `Could not resolve placeholder 'DB_PASSWORD'`
o similar; ese es el comportamiento esperado.
