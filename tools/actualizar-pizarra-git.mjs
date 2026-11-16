// Genera historial-git.js, el archivo que la pizarra Scrum lee al abrirse.
// Lo ejecuta el hook post-commit (y también se puede correr a mano):
//   node tools/actualizar-pizarra-git.mjs
//
// La pizarra ya no vive dentro del repositorio: está en la carpeta de OneDrive
// del escritorio. PIZARRA_DIR permite apuntar a otra carpeta sin tocar código.
import { execFileSync } from 'node:child_process';
import { existsSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const raiz = dirname(dirname(fileURLToPath(import.meta.url)));

const CARPETA_POR_DEFECTO = 'C:\\Users\\brony\\OneDrive\\Escritorio\\PIZARRA SCRUM';
const carpeta = process.env.PIZARRA_DIR || CARPETA_POR_DEFECTO;
const destino = join(carpeta, 'historial-git.js');

if (!existsSync(carpeta)) {
    console.error('No encuentro la carpeta de la pizarra: ' + carpeta);
    console.error('Créala o indica otra con la variable PIZARRA_DIR.');
    process.exit(1);
}

const git = args => execFileSync('git', args, { cwd: raiz, encoding: 'utf8' }).trim();

let rama = '?';
let commits = [];

try {
    rama = git(['rev-parse', '--abbrev-ref', 'HEAD']) || '?';
    const crudo = execFileSync(
        'git',
        ['log', '-n', '200', '--date=format:%Y-%m-%d %H:%M', '--pretty=format:%H%x1f%h%x1f%ad%x1f%an%x1f%s%x1e'],
        { cwd: raiz, encoding: 'utf8' }
    );
    commits = crudo
        .split('\x1e')
        .map(parte => parte.trim())
        .filter(Boolean)
        .map(linea => {
            const [hash, corto, fecha, autor, asunto] = linea.split('\x1f');
            return { hash, corto, fecha, autor, asunto };
        });
} catch (error) {
    console.error('No pude leer el historial de Git:', error.message);
}

const datos = {
    generado: new Date().toISOString(),
    rama,
    total: commits.length,
    commits
};

const contenido = [
    '// Archivo generado automáticamente por tools/actualizar-pizarra-git.mjs.',
    '// No lo edites a mano: el hook post-commit lo reescribe en cada commit.',
    'window.HISTORIAL_GIT = ' + JSON.stringify(datos, null, 2) + ';',
    ''
].join('\n');

writeFileSync(destino, contenido, 'utf8');
console.log('Pizarra enlazada con Git: ' + commits.length + ' commits de ' + rama + ' en ' + destino);
