// Sirve la carpeta de la pizarra Scrum para abrirla con su historial de Git.
// La página necesita leer historial-git.js, que es un archivo hermano, y eso
// solo funciona desde http://; este servidor es el camino corto:
//
//   node tools/servir-pizarra.mjs        (y abre http://127.0.0.1:4173/pizarra-scrum.html)
//
// La pizarra vive en OneDrive/Escritorio/PIZARRA SCRUM. PIZARRA_DIR permite
// servir otra carpeta (por ejemplo docs/ si vuelves a tenerla en el repo).
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, normalize, extname } from 'node:path';

const CARPETA_POR_DEFECTO = 'C:\\Users\\brony\\OneDrive\\Escritorio\\PIZARRA SCRUM';
const raizProyecto = dirname(dirname(fileURLToPath(import.meta.url)));

const preferida = process.env.PIZARRA_DIR || CARPETA_POR_DEFECTO;
const docs = existsSync(join(preferida, 'pizarra-scrum.html'))
    ? preferida
    : join(raizProyecto, 'docs');

const puerto = Number(process.env.PUERTO || 4173);

const tipos = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

const servidor = createServer(async (req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    let ruta = decodeURIComponent(url.pathname);
    if (ruta === '/' || ruta === '') ruta = '/pizarra-scrum.html';

    const destino = normalize(join(docs, ruta));
    if (!destino.startsWith(docs)) {
        res.writeHead(403, { 'content-type': 'text/plain; charset=utf-8' });
        res.end('Fuera de la carpeta de la pizarra');
        return;
    }

    try {
        const cuerpo = await readFile(destino);
        res.writeHead(200, {
            'content-type': tipos[extname(destino).toLowerCase()] || 'application/octet-stream',
            'cache-control': 'no-store'
        });
        res.end(cuerpo);
    } catch (error) {
        res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
        res.end('No encontrado: ' + ruta);
    }
});

servidor.listen(puerto, '127.0.0.1', () => {
    console.log('Sirviendo: ' + docs);
    console.log('Pizarra en http://127.0.0.1:' + puerto + '/pizarra-scrum.html');
    console.log('Ctrl+C para detener el servidor.');
});
