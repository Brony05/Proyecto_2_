// ==================== COMITÉ DE ÉTICA - VARIABLES GLOBALES ====================
let listaCasos = [];
let listaQuejas = [];

// ==================== COMITÉ DE ÉTICA - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'COMITE_ETICA') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('userName').innerText = localStorage.getItem('username');
    cargarModoOscuro();
    cargarDashboardEtica();
    cargarCasos();
    cargarPacientesEtica();
    cargarQuejas();
    
    // Eventos de filtros
    document.getElementById('buscarCaso')?.addEventListener('input', cargarCasos);
    document.getElementById('filtroEstadoCaso')?.addEventListener('change', cargarCasos);
    document.getElementById('buscarPacienteEtica')?.addEventListener('input', cargarPacientesEtica);
    document.getElementById('buscarQueja')?.addEventListener('input', cargarQuejas);
    document.getElementById('filtroEstadoQueja')?.addEventListener('change', cargarQuejas);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            document.getElementById(tab).style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardEtica();
            if (tab === 'casos') cargarCasos();
            if (tab === 'pacientes') cargarPacientesEtica();
            if (tab === 'quejas') cargarQuejas();
        });
    });
});

// ==================== COMITÉ DE ÉTICA - DASHBOARD ====================
async function cargarDashboardEtica() {
    setLoading(true);
    try {
        // Simular datos
        const total = 24;
        const pendientes = 8;
        const aprobados = 12;
        const rechazados = 4;
        
        document.getElementById('totalCasos').innerText = total;
        document.getElementById('casosPendientes').innerText = pendientes;
        document.getElementById('casosAprobados').innerText = aprobados;
        document.getElementById('casosRechazados').innerText = rechazados;
        
        // Casos recientes
        const casosRecientes = [
            { id: 1, paciente: 'Juan Pérez', tipo: 'Evaluación Psiquiátrica', fecha: '2024-01-15', estado: 'PENDIENTE' },
            { id: 2, paciente: 'María García', tipo: 'Terapia de Pareja', fecha: '2024-01-14', estado: 'APROBADO' },
            { id: 3, paciente: 'Carlos López', tipo: 'Psicología Infantil', fecha: '2024-01-13', estado: 'EN_REVISION' },
            { id: 4, paciente: 'Ana Torres', tipo: 'Psiquiatría', fecha: '2024-01-12', estado: 'RECHAZADO' },
            { id: 5, paciente: 'Roberto Mendoza', tipo: 'Neuropsicología', fecha: '2024-01-11', estado: 'PENDIENTE' },
        ];
        
        document.getElementById('tbodyCasosRecientes').innerHTML = casosRecientes.map(c => {
            const estadoClass = c.estado === 'PENDIENTE' ? 'badge-pendiente' : 
                               c.estado === 'APROBADO' ? 'badge-aprobado' : 
                               c.estado === 'RECHAZADO' ? 'badge-rechazado' : 'badge-en-revision';
            return `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.paciente}</td>
                    <td>${c.tipo}</td>
                    <td>${c.fecha}</td>
                    <td><span class="badge-estado-caso ${estadoClass}">${c.estado}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="revisarCaso(${c.id}, '${c.paciente}', '${c.tipo}', '${c.fecha}', 'Descripción del caso ${c.id}')">Revisar</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== COMITÉ DE ÉTICA - CASOS ====================
async function cargarCasos() {
    setLoading(true);
    try {
        // Simular casos
        const casos = [
            { id: 1, paciente: 'Juan Pérez', descripcion: 'Caso de evaluación psiquiátrica para tratamiento', tipo: 'Evaluación Psiquiátrica', fecha: '2024-01-15', estado: 'PENDIENTE' },
            { id: 2, paciente: 'María García', descripcion: 'Terapia de pareja con seguimiento', tipo: 'Terapia de Pareja', fecha: '2024-01-14', estado: 'APROBADO' },
            { id: 3, paciente: 'Carlos López', descripcion: 'Caso de psicología infantil', tipo: 'Psicología Infantil', fecha: '2024-01-13', estado: 'EN_REVISION' },
            { id: 4, paciente: 'Ana Torres', descripcion: 'Caso de psiquiatría con medicación', tipo: 'Psiquiatría', fecha: '2024-01-12', estado: 'RECHAZADO' },
            { id: 5, paciente: 'Roberto Mendoza', descripcion: 'Evaluación neuropsicológica', tipo: 'Neuropsicología', fecha: '2024-01-11', estado: 'PENDIENTE' },
            { id: 6, paciente: 'Laura Fernández', descripcion: 'Caso de ansiedad generalizada', tipo: 'Psicología Clínica', fecha: '2024-01-10', estado: 'APROBADO' },
            { id: 7, paciente: 'Pedro Sánchez', descripcion: 'Caso de depresión mayor', tipo: 'Psiquiatría', fecha: '2024-01-09', estado: 'EN_REVISION' },
        ];
        
        const termino = document.getElementById('buscarCaso')?.value || '';
        const estado = document.getElementById('filtroEstadoCaso')?.value;
        
        let filtered = casos;
        if (termino.length > 1) {
            filtered = filtered.filter(c => c.paciente.toLowerCase().includes(termino.toLowerCase()));
        }
        if (estado) {
            filtered = filtered.filter(c => c.estado === estado);
        }
        
        document.getElementById('tbodyCasos').innerHTML = filtered.map(c => {
            const estadoClass = c.estado === 'PENDIENTE' ? 'badge-pendiente' : 
                               c.estado === 'APROBADO' ? 'badge-aprobado' : 
                               c.estado === 'RECHAZADO' ? 'badge-rechazado' : 'badge-en-revision';
            const cardClass = c.estado === 'PENDIENTE' ? 'caso-pendiente' : 
                             c.estado === 'APROBADO' ? 'caso-aprobado' : 
                             c.estado === 'RECHAZADO' ? 'caso-rechazado' : 'caso-en-revision';
            return `
                <tr>
                    <td>${c.id}</td>
                    <td>${c.paciente}</td>
                    <td>${c.descripcion}</td>
                    <td>${c.tipo}</td>
                    <td>${c.fecha}</td>
                    <td><span class="badge-estado-caso ${estadoClass}">${c.estado}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="revisarCaso(${c.id}, '${c.paciente}', '${c.tipo}', '${c.fecha}', '${c.descripcion}')">Revisar</button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch(e) { console.error(e); }
    setLoading(false);
}

function revisarCaso(id, paciente, tipo, fecha, descripcion) {
    document.getElementById('casoId').value = id;
    document.getElementById('casoPaciente').innerText = paciente;
    document.getElementById('casoFecha').innerText = fecha;
    document.getElementById('casoTipo').innerText = tipo;
    document.getElementById('casoDescripcion').innerText = descripcion;
    document.getElementById('casoDictamen').value = '';
    document.getElementById('casoDecision').value = 'APROBADO';
    new bootstrap.Modal(document.getElementById('casoModal')).show();
}

function guardarCaso() {
    const id = document.getElementById('casoId').value;
    const dictamen = document.getElementById('casoDictamen').value;
    const decision = document.getElementById('casoDecision').value;
    
    if (!dictamen) {
        mostrarMensaje('❌ Escribe un dictamen', 'danger');
        return;
    }
    
    mostrarMensaje(`✅ Caso ${id} ${decision === 'APROBADO' ? 'aprobado' : decision === 'RECHAZADO' ? 'rechazado' : 'enviado a revisión'}`, 'success');
    bootstrap.Modal.getInstance(document.getElementById('casoModal')).hide();
    cargarCasos();
    cargarDashboardEtica();
}

// ==================== COMITÉ DE ÉTICA - PACIENTES ====================
async function cargarPacientesEtica() {
    setLoading(true);
    try {
        const res = await authFetch('/api/clientes');
        if (res.ok) {
            let pacientes = await res.json();
            const termino = document.getElementById('buscarPacienteEtica')?.value || '';
            
            if (termino.length > 1) {
                pacientes = pacientes.filter(p => p.nombre.toLowerCase().includes(termino.toLowerCase()));
            }
            
            document.getElementById('tbodyPacientesEtica').innerHTML = pacientes.map(p => `
                <tr>
                    <td>${p.id}</td>
                    <td>${p.nombre}</td>
                    <td>${p.telefono || 'N/A'}</td>
                    <td>${p.correo || 'N/A'}</td>
                    <td>${Math.floor(Math.random() * 5) + 1}</td>
                    <td>
                        <button class="btn btn-sm btn-info" onclick="verCasosPaciente(${p.id})">Ver Casos</button>
                    </td>
                </tr>
            `).join('');
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

function verCasosPaciente(id) {
    mostrarMensaje(`📋 Viendo casos del paciente ${id}`, 'info');
}

// ==================== COMITÉ DE ÉTICA - QUEJAS ====================
async function cargarQuejas() {
    setLoading(true);
    try {
        const quejas = [
            { id: 1, paciente: 'Juan Pérez', asunto: 'Demora en atención', fecha: '2024-01-15', estado: 'PENDIENTE' },
            { id: 2, paciente: 'María García', asunto: 'Falta de seguimiento', fecha: '2024-01-14', estado: 'EN_PROCESO' },
            { id: 3, paciente: 'Carlos López', asunto: 'Trato inadecuado', fecha: '2024-01-13', estado: 'RESUELTO' },
            { id: 4, paciente: 'Ana Torres', asunto: 'Error en medicación', fecha: '2024-01-12', estado: 'PENDIENTE' },
            { id: 5, paciente: 'Roberto Mendoza', asunto: 'Falta de comunicación', fecha: '2024-01-11', estado: 'EN_PROCESO' },
        ];
        
        const termino = document.getElementById('buscarQueja')?.value || '';
        const estado = document.getElementById('filtroEstadoQueja')?.value;
        
        let filtered = quejas;
        if (termino.length > 1) {
            filtered = filtered.filter(q => q.paciente.toLowerCase().includes(termino.toLowerCase()));
        }
        if (estado) {
            filtered = filtered.filter(q => q.estado === estado);
        }
        
        document.getElementById('tbodyQuejas').innerHTML = filtered.map(q => `
            <tr>
                <td>${q.id}</td>
                <td>${q.paciente}</td>
                <td>${q.asunto}</td>
                <td>${q.fecha}</td>
                <td><span class="badge ${q.estado === 'PENDIENTE' ? 'bg-warning' : q.estado === 'EN_PROCESO' ? 'bg-info' : 'bg-success'}">${q.estado}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="atenderQueja(${q.id})">Atender</button>
                </td>
            </tr>
        `).join('');
    } catch(e) { console.error(e); }
    setLoading(false);
}

function atenderQueja(id) {
    mostrarMensage(`📋 Atendiendo queja ${id}`, 'info');
}