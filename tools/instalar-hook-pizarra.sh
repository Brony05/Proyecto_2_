#!/bin/sh
# Engancha la pizarra Scrum al historial de Git de este repositorio.
#
#   sh tools/instalar-hook-pizarra.sh
#
# Añade un bloque a los hooks post-commit, post-merge y post-checkout que
# regenera docs/historial-git.js. El bloque es idempotente y respeta hooks
# que ya existan. Para desinstalar, borra las líneas entre los marcadores
# «pizarra-scrum» de cada archivo en .git/hooks.
set -e

RAIZ=$(git rev-parse --show-toplevel)
HOOKS="$RAIZ/.git/hooks"

if [ ! -d "$HOOKS" ]; then
    echo "No encuentro $HOOKS. Ejecuta este script dentro del repositorio."
    exit 1
fi

if ! command -v node >/dev/null 2>&1; then
    echo "Necesito Node.js en el PATH para generar el historial."
    exit 1
fi

echo "Instalando hooks de la pizarra en $HOOKS"

for NOMBRE in post-commit post-merge post-checkout; do
    ARCHIVO="$HOOKS/$NOMBRE"

    if [ ! -f "$ARCHIVO" ]; then
        printf '#!/bin/sh\n' > "$ARCHIVO"
        echo "  $NOMBRE creado"
    fi

    if grep -q 'pizarra-scrum' "$ARCHIVO" 2>/dev/null; then
        echo "  $NOMBRE ya estaba enganchado"
    else
        cat >> "$ARCHIVO" <<'BLOQUE'

# >>> pizarra-scrum: mantiene el historial de Git en docs/historial-git.js
raiz=$(git rev-parse --show-toplevel 2>/dev/null) && node "$raiz/tools/actualizar-pizarra-git.mjs" >/dev/null 2>&1 || true
# <<< pizarra-scrum
BLOQUE
        echo "  $NOMBRE enganchado"
    fi

    chmod +x "$ARCHIVO" 2>/dev/null || true
done

node "$RAIZ/tools/actualizar-pizarra-git.mjs"
echo "Listo. Cada commit nuevo aparecerá en la pizarra al recargarla."
