#!/bin/bash
# Script: clean-ignored.sh
# Propósito: eliminar del índice todos los archivos rastreados que deberían estar ignorados según .gitignore

# Asegúrate de estar en la raíz del repo
cd "$(git rev-parse --show-toplevel)" || exit 1

echo "🔍 Buscando archivos rastreados que coinciden con .gitignore..."

# Lista todos los archivos ignorados que ya están en el índice
git ls-files -i --exclude-from=.gitignore > ignored-tracked.txt

if [ -s ignored-tracked.txt ]; then
  echo "📂 Archivos a eliminar del índice:"
  cat ignored-tracked.txt

  # Elimina del índice (no del disco)
  git rm -r --cached $(cat ignored-tracked.txt)

  echo "✅ Archivos eliminados del índice. Haz commit para aplicar los cambios."
else
  echo "👌 No hay archivos rastreados que coincidan con .gitignore."
fi

# Limpieza
rm -f ignored-tracked.txt