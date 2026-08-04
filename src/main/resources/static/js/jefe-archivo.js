// ==================== JEFE ARCHIVO - VARIABLES GLOBALES ====================
let listaExpedientes = [];
let estadoExpediente = {};

// ==================== JEFE ARCHIVO - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'JEFE_ARCHIVO') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardArchivo();
    cargarExpedientes();
    cargarPacientesArchivo();
    cargarAuditoria();
    
    // Eventos de filtros
    document.getElementById('buscarExpediente')?.addEventListener('input', cargarExpedientes);
    document.getElementById('filtroEstadoExpediente')?.addEventListener('change', cargarExpedientes);
    document.getElementById('buscarPacienteArchivo')?.addEventListener('input', cargarPacientesArchivo);
    document.getElementById('auditoriaFecha')?.addEventListener('change', cargarAuditoria);
    document.getElementById('auditoriaTipo')?.addEventListener('change', cargarAuditoria);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardArchivo();
            if (tab === 'expedientes') cargarExpedientes();
            if (tab === 'pacientes') cargarPacientesArchivo();
            if (tab === 'auditoria') cargarAuditoria();
        });
    });
});

// ==================== JEFE ARCHIVO - DASHBOARD ====================
async function cargarDashboardArchivo() {
    setLoading(true);
    try {
        const resPacientes = await authFetch('/api/clientes');
        if (resPacientes.ok) {
            const pacientes = await resPacientes.json();
            document.getElementById('totalExpedientes').innerText = pacientes.length;
            
            // Simular estados de expedientes
            const activos = Math.floor(pacientes.length * 0.7);
            const archivados = Math.floor(pacientes.length * 0.2);
            document.getElementById('expedientesActivos').innerText = activos;
            document.getElementById('expedientesArchivados').innerText = archivados;
        }
        
        const resNotas = await authFetch('/api/notas/doctor/paciente/1').catch(() => null);
        if (resNotas && resNotas.ok) {
            const notas = await resNotas.json();
            document.getElementById('notasRegistradas').innerText = notas.length;
        }
        
        // Últimos expedientes modificados (simulado)
        const resCitas = await authFetch('/api/citas');
        if (resCitas.ok) {
            const citas = await resCitas.json();
            const pacientesRecientes = [];
            const vistos = new Set();
            for (const c of citas.reverse()) {
                if (!vistos.has(c.cliente?.id) && c.cliente) {
                    vistos.add(c.cliente.id);
                    pacientesRecientes.push(c.cliente);
                    if (pacientesRecientes.length >= 5) break;
                }
            }
            
            document.getElementById('tbodyUltimosExpedientes').innerHTML = pacientesRecientes.map(p => `
                <tr>
                    <td>${p.nombre}</td>
                    <td>${new Date().toLocaleDateString()}</td>
                    <td><span class="archivo-badge archivo-activo">Activo</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="verExpedientePaciente(${p.id})">Ver</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== JEFE ARCHIVO - EXPEDIENTES ====================
async function cargarExpedientes() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            let pacientes = await res.json();
            const termino = document.getElementById('buscarExpediente')?.value || '';
            const estado = document.getElementById('filtroEstadoExpediente')?.value;
            
            if (termino.length > 1) {
                pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
            }
            
            // Simular estado de expedientes
            const estados = ['ACTIVO', 'ACTIVO', 'ACTIVO', 'INACTIVO', 'ARCHIVADO'];
            const notasCount = [3, 5, 2, 8, 1, 4, 6, 2, 3, 7];
            
            let filtered = pacientes;
            if (estado) {
                filtered = pacientes.filter((p, i) => {
                    const estadoPaciente = estados[i % estados.length];
                    return estadoPaciente === estado;
                });
            }
            
            document.getElementById('tbodyExpedientes').innerHTML = filtered.map((p, i) => {
                const estadoPaciente = estados[i % estados.length];
                const estadoClass = estadoPaciente === 'ACTIVO' ? 'archivo-activo' : 
                                   estadoPaciente === 'INACTIVO' ? 'archivo-inactivo' : 'archivo-archivado';
                return `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.nombre}</td>
                        <td>${p.telefono || 'N/A'}</td>
                        <td>${p.correo || 'N/A'}</td>
                        <td><span class="archivo-badge ${estadoClass}">${estadoPaciente}</span></td>
                        <td>${notasCount[i % notasCount.length]}</td>
                        <td>${new Date().toLocaleDateString()}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="editarExpediente(${p.id}, '${p.nombre}')">Gestionar</button>
                            <button class="btn btn-sm btn-info" onclick="verExpedientePaciente(${p.id})">Ver</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function editarExpediente(id, nombre) {
    document.getElementById('expedientePacienteId').value = id;
    document.getElementById('expedientePacienteNombre').innerText = nombre;
    document.getElementById('expedienteEstado').value = 'ACTIVO';
    document.getElementById('expedienteNotas').value = '';
    new bootstrap.Modal(document.getElementById('expedienteModal')).show();
}

function guardarExpediente() {
    const pacienteId = document.getElementById('expedientePacienteId').value;
    const estado = document.getElementById('expedienteEstado').value;
    const notas = document.getElementById('expedienteNotas').value;
    
    mostrarMensaje(`✅ Expediente de paciente ${pacienteId} actualizado a ${estado}`, 'success');
    bootstrap.Modal.getInstance(document.getElementById('expedienteModal')).hide();
    cargarExpedientes();
    cargarDashboardArchivo();
}

function verExpedientePaciente(id) {
    mostrarMensaje(`📋 Viendo expediente completo del paciente ${id}`, 'info');
}

// ==================== JEFE ARCHIVO - PACIENTES ====================
async function cargarPacientesArchivo() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            let pacientes = await res.json();
            const termino = document.getElementById('buscarPacienteArchivo')?.value || '';
            
            if (termino.length > 1) {
                pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
            }
            
            // Simular estados
            const estados = ['ACTIVO', 'ACTIVO', 'INACTIVO', 'ARCHIVADO', 'ACTIVO'];
            
            // Obtener notas
            const resNotas = await authFetch('/api/notas/doctor/paciente/1').catch(() => null);
            let totalNotas = 0;
            if (resNotas && resNotas.ok) {
                const notas = await resNotas.json();
                totalNotas = notas.length;
            }
            
            document.getElementById('tbodyPacientesArchivo').innerHTML = pacientes.map((p, i) => {
                const estado = estados[i % estados.length];
                const estadoClass = estado === 'ACTIVO' ? 'archivo-activo' : 
                                   estado === 'INACTIVO' ? 'archivo-inactivo' : 'archivo-archivado';
                return `
                    <tr>
                        <td>${p.id}</td>
                        <td>${p.nombre}</td>
                        <td>${p.telefono || 'N/A'}</td>
                        <td>${p.correo || 'N/A'}</td>
                        <td><span class="archivo-badge ${estadoClass}">${estado}</span></td>
                        <td>${totalNotas || Math.floor(Math.random() * 10) + 1}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="editarExpediente(${p.id}, '${p.nombre}')">Gestionar</button>
                            <button class="btn btn-sm btn-info" onclick="verExpedientePaciente(${p.id})">Ver</button>
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== JEFE ARCHIVO - AUDITORÍA ====================
async function cargarAuditoria() {
    setLoading(true);
    try {
        const fecha = document.getElementById('auditoriaFecha')?.value;
        const tipo = document.getElementById('auditoriaTipo')?.value;
        
        // Simular auditoría
        const acciones = ['CREACION', 'MODIFICACION', 'CONSULTA', 'ARCHIVO', 'MODIFICACION', 'CONSULTA'];
        const usuarios = ['admin', 'psicologo1', 'psiquiatra1', 'enfermero1', 'director'];
        const pacientes = ['Juan Pérez', 'María García', 'Carlos López', 'Ana Torres'];
        const detalles = [
            'Creación de expediente',
            'Actualización de diagnóstico',
            'Consulta de historial',
            'Archivo de expediente',
            'Modificación de datos',
            'Consulta de notas'
        ];
        
        let items = [];
        const hoy = new Date();
        for (let i = 0; i < 15; i++) {
            const d = new Date(hoy);
            d.setHours(d.getHours() - i * 2);
            items.push({
                fecha: d,
                usuario: usuarios[i % usuarios.length],
                paciente: pacientes[i % pacientes.length],
                accion: acciones[i % acciones.length],
                detalle: detalles[i % detalles.length]
            });
        }
        
        if (fecha) {
            items = items.filter(item => item.fecha.toISOString().split('T')[0] === fecha);
        }
        if (tipo) {
            items = items.filter(item => item.accion === tipo);
        }
        
        document.getElementById('tbodyAuditoria').innerHTML = items.map(item => `
            <tr>
                <td>${item.fecha.toLocaleString()}</td>
                <td>${item.usuario}</td>
                <td>${item.paciente}</td>
                <td><span class="badge ${item.accion === 'CREACION' ? 'bg-success' : item.accion === 'MODIFICACION' ? 'bg-warning' : item.accion === 'ARCHIVO' ? 'bg-danger' : 'bg-info'}">${item.accion}</span></td>
                <td>${item.detalle}</td>
            </tr>
        `).join('');
        
    } catch(e) { console.error(e); }
    setLoading(false);
}