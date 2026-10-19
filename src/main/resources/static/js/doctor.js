// ==================== DOCTOR - VARIABLES GLOBALES ====================
let calendar = null;
let citaActualId = null;
let pacienteSeleccionadoIdNota = null;

// ==================== CRONÓMETRO EN REVERSA ====================
let cronometroInterval = null;
let tiempoRestanteSegundos = 0;
let tiempoTotalSegundos = 0;
let cronometroActivo = false;

function formatoTiempo(segundos) {
    const mins = Math.floor(segundos / 60);
    const secs = segundos % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
}

function actualizarCronometro() {
    const tiempoElem = document.getElementById('cronometroTiempo');
    if (tiempoElem) tiempoElem.innerText = formatoTiempo(tiempoRestanteSegundos);
    
    const alertaDiv = document.getElementById('alertaTiempo');
    if (alertaDiv) {
        if (tiempoRestanteSegundos <= 300 && tiempoRestanteSegundos > 0) {
            alertaDiv.style.display = 'block';
        } else {
            alertaDiv.style.display = 'none';
        }
    }
    
    if (tiempoRestanteSegundos <= 0 && cronometroActivo) {
        pausarCronometro();
        mostrarMensaje('⏰ El tiempo de la consulta ha terminado.', 'warning');
        const btnIniciar = document.getElementById('btnIniciarCrono');
        if (btnIniciar) btnIniciar.disabled = true;
    }
}

function iniciarCronometro() {
    if (cronometroInterval) clearInterval(cronometroInterval);
    const tiempoProgramado = parseInt(document.getElementById('tiempoProgramado')?.value || 45);
    if (tiempoRestanteSegundos === 0) {
        tiempoTotalSegundos = tiempoProgramado * 60;
        tiempoRestanteSegundos = tiempoTotalSegundos;
        actualizarCronometro();
    }
    cronometroActivo = true;
    cronometroInterval = setInterval(() => {
        if (cronometroActivo && tiempoRestanteSegundos > 0) {
            tiempoRestanteSegundos--;
            actualizarCronometro();
        } else if (tiempoRestanteSegundos <= 0) {
            pausarCronometro();
            mostrarMensaje('⏰ El tiempo de la consulta ha terminado.', 'warning');
        }
    }, 1000);
    
    const btnIniciar = document.getElementById('btnIniciarCrono');
    const btnPausar = document.getElementById('btnPausarCrono');
    const btnReiniciar = document.getElementById('btnReiniciarCrono');
    if (btnIniciar) btnIniciar.disabled = true;
    if (btnPausar) btnPausar.disabled = false;
    if (btnReiniciar) btnReiniciar.disabled = false;
}

function pausarCronometro() {
    cronometroActivo = false;
    if (cronometroInterval) clearInterval(cronometroInterval);
    const btnIniciar = document.getElementById('btnIniciarCrono');
    const btnPausar = document.getElementById('btnPausarCrono');
    if (btnIniciar) btnIniciar.disabled = false;
    if (btnPausar) btnPausar.disabled = true;
}

function reiniciarCronometro() {
    const tiempoProgramado = parseInt(document.getElementById('tiempoProgramado')?.value || 45);
    tiempoTotalSegundos = tiempoProgramado * 60;
    tiempoRestanteSegundos = tiempoTotalSegundos;
    actualizarCronometro();
    if (!cronometroActivo) {
        cronometroActivo = true;
        iniciarCronometro();
    }
}

function extenderTiempo() {
    const minutosExtra = parseInt(document.getElementById('extenderTiempo')?.value || 15);
    const costoExtra = minutosExtra === 15 ? 200 : minutosExtra === 30 ? 350 : 600;
    if (confirm(`Extender la cita ${minutosExtra} minutos con un costo adicional de $${costoExtra}?`)) {
        tiempoRestanteSegundos += minutosExtra * 60;
        tiempoTotalSegundos += minutosExtra * 60;
        actualizarCronometro();
        mostrarMensaje(`✅ Tiempo extendido +${minutosExtra} minutos. Costo adicional: $${costoExtra}`, 'success');
    }
}

async function finalizarConsulta() {
    if (!confirm('¿Finalizar esta consulta? Se marcará como completada y se guardarán las notas.')) return;
    
    const tiempoUsado = tiempoTotalSegundos - tiempoRestanteSegundos;
    const tiempoTotal = formatoTiempo(tiempoUsado);
    const notas = document.getElementById('notasConsulta')?.value || '';
    
    if (notas) {
        console.log('Notas de la consulta:', notas);
    }
    
    if (cronometroInterval) clearInterval(cronometroInterval);
    
    const exito = await cambiarEstado(citaActualId, 'COMPLETADA');
    
    if (exito) {
        mostrarMensaje(`✅ Consulta finalizada. Duración total: ${tiempoTotal}`, 'success');
        const modal = bootstrap.Modal.getInstance(document.getElementById('iniciarCitaModal'));
        if (modal) modal.hide();
        const notasInput = document.getElementById('notasConsulta');
        if (notasInput) notasInput.value = '';
        tiempoRestanteSegundos = 0;
        tiempoTotalSegundos = 0;
        actualizarCronometro();
        cronometroActivo = false;
    } else {
        mostrarMensaje('❌ Error al finalizar la consulta', 'danger');
    }
}

function abrirIniciarCita(id) {
    citaActualId = id;
    tiempoRestanteSegundos = 0;
    tiempoTotalSegundos = 0;
    actualizarCronometro();
    if (cronometroInterval) clearInterval(cronometroInterval);
    cronometroActivo = false;
    
    const btnIniciar = document.getElementById('btnIniciarCrono');
    const btnPausar = document.getElementById('btnPausarCrono');
    const btnReiniciar = document.getElementById('btnReiniciarCrono');
    if (btnIniciar) btnIniciar.disabled = false;
    if (btnPausar) btnPausar.disabled = true;
    if (btnReiniciar) btnReiniciar.disabled = true;
    
    const notasInput = document.getElementById('notasConsulta');
    if (notasInput) notasInput.value = '';
    
    const alertaDiv = document.getElementById('alertaTiempo');
    if (alertaDiv) alertaDiv.style.display = 'none';
    
    new bootstrap.Modal(document.getElementById('iniciarCitaModal')).show();
}

// Configurar botones del cronómetro
document.addEventListener('DOMContentLoaded', function() {
    const btnIniciarCrono = document.getElementById('btnIniciarCrono');
    const btnPausarCrono = document.getElementById('btnPausarCrono');
    const btnReiniciarCrono = document.getElementById('btnReiniciarCrono');
    
    if (btnIniciarCrono) btnIniciarCrono.addEventListener('click', iniciarCronometro);
    if (btnPausarCrono) btnPausarCrono.addEventListener('click', pausarCronometro);
    if (btnReiniciarCrono) btnReiniciarCrono.addEventListener('click', reiniciarCronometro);
});

// ==================== DOCTOR - INICIALIZACIÓN ====================
document.addEventListener('DOMContentLoaded', function() {
    const sesion = verificarSesion();
    if (!sesion || sesion.rol !== 'DOCTOR') {
        if (sesion) redirigirPorRol(sesion.rol);
        return;
    }
    
    document.getElementById('doctorName').innerText = localStorage.getItem('username');
    document.getElementById('doctorEspecialidad').innerText = localStorage.getItem('especialidad') || 'Médico General';
    
    cargarDashboardDoctor();
    cargarCitasDoctor();
    cargarPacientesDoctor();
    cargarPacientesParaNotas();
    
    // Eventos de filtros
    const filtroFecha = document.getElementById('filtroFecha');
    const filtroEstado = document.getElementById('filtroEstado');
    const filtroEspecialidad = document.getElementById('filtroEspecialidad');
    const buscarPaciente = document.getElementById('buscarPaciente');
    const buscarPacienteNota = document.getElementById('buscarPacienteNota');
    
    if (filtroFecha) filtroFecha.addEventListener('change', () => cargarCitasDoctor());
    if (filtroEstado) filtroEstado.addEventListener('change', () => cargarCitasDoctor());
    if (filtroEspecialidad) filtroEspecialidad.addEventListener('change', () => cargarCitasDoctor());
    if (buscarPaciente) buscarPaciente.addEventListener('input', buscarPaciente);
    if (buscarPacienteNota) buscarPacienteNota.addEventListener('input', buscarPacienteNota);
    
    // Tabs
    document.querySelectorAll('[data-tab]').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.getAttribute('data-tab');
            document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
            const tabContent = document.getElementById(tab);
            if (tabContent) tabContent.style.display = 'block';
            document.querySelectorAll('.nav-link').forEach(n => n.classList.remove('active'));
            btn.classList.add('active');
            
            if (tab === 'dashboard') cargarDashboardDoctor();
            if (tab === 'calendario') cargarCalendario();
            if (tab === 'citas') cargarCitasDoctor();
            if (tab === 'pacientes') cargarPacientesDoctor();
            if (tab === 'notas') cargarPacientesParaNotas();
        });
    });
});

// ==================== DOCTOR - FUNCIONES AUXILIARES ====================
// Servicios del catálogo realista de hospital psiquiátrico que corresponden a cada especialidad.
// (Ya no se usa para filtrar la agenda del doctor: /api/citas ya devuelve solo sus citas;
//  queda como referencia de qué servicios atiende cada especialidad.)
function obtenerServiciosPorEspecialidad(especialidad) {
    const mapa = {
        'Psicología Clínica': ['Valoración Psicológica', 'Consulta Inicial', 'Terapia Psicológica Individual', 'Terapia de Seguimiento'],
        'Psiquiatría': ['Consulta de Psiquiatría', 'Consulta de Seguimiento Psiquiátrico', 'Urgencia Psiquiátrica'],
        'Psicología Infantil': ['Valoración Psicológica Infantil', 'Terapia Infantil y del Adolescente'],
        'Terapia de Pareja': ['Terapia de Pareja'],
        'Neuropsicología': ['Valoración Neuropsicológica', 'Neuropsicología Clínica']
    };
    return mapa[especialidad] || [];
}

async function cambiarEstado(id, nuevoEstado) {
    let msg = '';
    if (nuevoEstado === 'EN_PROGRESO') msg = '¿Iniciar esta terapia?';
    else if (nuevoEstado === 'COMPLETADA') msg = '¿Marcar esta cita como completada?';
    else msg = `¿Cambiar estado a ${nuevoEstado}?`;
    
    if (!confirm(msg)) return false;
    
    setLoading(true);
    try {
        const token = getToken();
        console.log('🔵 Token actual:', token ? token.substring(0, 50) + '...' : 'null');
        console.log('🔵 Rol del usuario:', localStorage.getItem('rol'));
        
        const url = `/api/citas/${id}/estado?estado=${nuevoEstado}`;
        console.log('🔵 Enviando petición PUT a:', url);
        
        const res = await authFetch(url, { method: 'PUT' });
        
        console.log('🔵 Respuesta status:', res.status);
        
        if (res.ok) {
            const data = await res.json();
            console.log('✅ Estado actualizado:', data);
            await cargarDashboardDoctor();
            await cargarCitasDoctor();
            if (calendar) cargarCalendario();
            mostrarMensaje(`✅ Cita ${nuevoEstado === 'EN_PROGRESO' ? 'iniciada' : 'completada'} correctamente`);
            return true;
        } else {
            const error = await res.text();
            console.error('❌ Error en respuesta:', error);
            mostrarMensaje('❌ ' + error, 'danger');
            return false;
        }
    } catch (error) {
        console.error('❌ Error de conexión:', error);
        mostrarMensaje('❌ Error de conexión', 'danger');
        return false;
    } finally {
        setLoading(false);
    }
}

// ==================== DOCTOR - DASHBOARD ====================
async function cargarDashboardDoctor() {
    try {
        const res = await authFetch('/api/citas');
        if (res.ok) {
            const todasCitas = await res.json();
            const hoy = new Date().toISOString().split('T')[0];
            // /api/citas ya devuelve SOLO las citas de este doctor (personal clínico):
            // no filtrar de nuevo por servicio/especialidad, es lo que hacía el dashboard vacío.
            const citasFiltradas = todasCitas;
            
            const citasHoy = citasFiltradas.filter(c => c.fecha === hoy && c.estado !== 'CANCELADA').length;
            const citasProgreso = citasFiltradas.filter(c => c.estado === 'EN_PROGRESO').length;
            const citasCompletadas = citasFiltradas.filter(c => c.estado === 'COMPLETADA').length;
            
            const citasHoyElem = document.getElementById('citasHoy');
            const citasProgresoElem = document.getElementById('citasProgreso');
            const citasCompletadasElem = document.getElementById('citasCompletadas');
            
            if (citasHoyElem) citasHoyElem.innerText = citasHoy;
            if (citasProgresoElem) citasProgresoElem.innerText = citasProgreso;
            if (citasCompletadasElem) citasCompletadasElem.innerText = citasCompletadas;
            
            const proximas = citasFiltradas.filter(c => c.fecha >= hoy && c.estado !== 'CANCELADA' && c.estado !== 'COMPLETADA').slice(0, 10);
            const tbodyProximasCitas = document.getElementById('tbodyProximasCitas');
            if (tbodyProximasCitas) {
                tbodyProximasCitas.innerHTML = proximas.map(c => `
                    <tr>
                        <td>${formatearFecha(c.fecha)}</td>
                        <td>${c.hora}</td>
                        <td>${c.cliente?.nombre || 'N/A'}</td>
                        <td>${c.servicio || 'N/A'}</td>
                        <td>${c.acompananteNombre ? c.acompananteNombre + ' (' + (c.acompananteRelacion || '-') + ')' : '-'}</td>
                        <td><span class="badge-estado ${c.estado === 'AGENDADA' ? 'badge-agendada' : 'badge-progreso'}">${c.estado === 'AGENDADA' ? 'Agendada' : 'En Progreso'}</span></td>
                        <td>${c.estado === 'AGENDADA' ? `<button class="btn btn-sm btn-success" onclick="abrirIniciarCita(${c.id})"><i class="fas fa-play"></i> Iniciar Consulta</button>` : ''}</td>
                    </tr>
                `).join('');
            }
        }
    } catch(e) { console.error(e); }
}

// ==================== DOCTOR - CITAS ====================
async function cargarCitasDoctor() {
    setLoading(true);
    try {
        const res = await authFetch('/api/citas');
        if (res.ok) {
            let citas = await res.json();
            // /api/citas ya devuelve SOLO las citas de este doctor:
            // no volver a filtrar por servicios de la especialidad (ocultaba citas reales).
            const estadoFiltro = document.getElementById('filtroEstado')?.value;
            const fechaFiltro = document.getElementById('filtroFecha')?.value;
            if (estadoFiltro) citas = citas.filter(c => c.estado === estadoFiltro);
            if (fechaFiltro) citas = citas.filter(c => c.fecha === fechaFiltro);
            
            const tbodyCitasDoctor = document.getElementById('tbodyCitasDoctor');
            if (tbodyCitasDoctor) {
                tbodyCitasDoctor.innerHTML = citas.map(c => {
                    const puedeIniciar = c.estado === 'AGENDADA';
                    const puedeCompletar = c.estado === 'EN_PROGRESO';
                    
                    return `
                        <tr>
                            <td>${c.id}</td>
                            <td>${formatearFecha(c.fecha)}</td>
                            <td>${c.hora}</td>
                            <td>${c.cliente?.nombre || 'N/A'}</td>
                            <td>${c.servicio || 'N/A'}</td>
                            <td>${c.cliente?.telefono || 'N/A'}</td>
                            <td>${obtenerBadgeEstado(c.estado)}</td>
                            <td>
                                ${puedeIniciar ? `<button class="btn btn-sm btn-success me-1" onclick="abrirIniciarCita(${c.id})"><i class="fas fa-play"></i> Iniciar</button>` : ''}
                                ${puedeCompletar ? `<button class="btn btn-sm btn-warning me-1" onclick="cambiarEstado(${c.id}, 'COMPLETADA')"><i class="fas fa-check"></i> Completar</button>` : ''}
                            </td>
                        </tr>
                    `;
                }).join('');
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

// ==================== DOCTOR - PACIENTES ====================
async function cargarPacientesDoctor() {
    setLoading(true);
    try {
        // Solo MIS pacientes asignados (por citas atendidas)
        const res = await authFetch('/api/clientes/mis-pacientes');
        if (res.ok) {
            const pacientes = await res.json();
            const citasRes = await authFetch('/api/citas');
            const todasCitas = await citasRes.json();
            
            const tbodyPacientesDoctor = document.getElementById('tbodyPacientesDoctor');
            if (tbodyPacientesDoctor) {
                if (pacientes.length === 0) {
                    tbodyPacientesDoctor.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Aún no tienes pacientes asignados. Aparecerán cuando atiendas citas.</td></tr>';
                } else {
                    tbodyPacientesDoctor.innerHTML = pacientes.map(p => {
                    const citasPaciente = todasCitas.filter(c => c.cliente?.id === p.id);
                    const ultimaCita = citasPaciente.sort((a,b) => new Date(b.fecha) - new Date(a.fecha))[0];
                    return `
                        <tr>
                            <td>${p.id}</td>
                            <td>${p.nombre}</td>
                            <td>${p.telefono || 'N/A'}</td>
                            <td>${p.correo || 'N/A'}</td>
                            <td>${citasPaciente.length}</td>
                            <td>${ultimaCita ? formatearFecha(ultimaCita.fecha) + ' ' + ultimaCita.hora : '-'}</td>
                            <td><button class="btn btn-sm btn-info" onclick="verNotasPaciente(${p.id}, '${p.nombre}')"><i class="fas fa-notes-medical"></i> Ver notas</button></td>
                        </tr>
                    `;
                }).join('');
                }
            }
        }
    } catch(e) { console.error(e); }
    setLoading(false);
}

async function buscarPaciente() {
    const termino = this.value.trim();
    if (termino.length > 1) {
        setLoading(true);
        try {
            // Búsqueda SOLO dentro de mis pacientes asignados
            const res = await authFetch('/api/clientes/mis-pacientes');
            if (res.ok) {
                const pacientes = (await res.json()).filter(p => (p.nombre || '').toLowerCase().includes(termino.toLowerCase()));
                const tbodyPacientesDoctor = document.getElementById('tbodyPacientesDoctor');
                if (tbodyPacientesDoctor) {
                    if (pacientes.length === 0) {
                        tbodyPacientesDoctor.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Sin resultados entre tus pacientes asignados.</td></tr>';
                    } else {
                    tbodyPacientesDoctor.innerHTML = pacientes.map(p => `
                        <tr>
                            <td>${p.id}</td>
                            <td>${p.nombre}</td>
                            <td>${p.telefono}</td>
                            <td>${p.correo}</td>
                            <td>-</td>
                            <td>-</td>
                            <td><button class="btn btn-sm btn-info" onclick="verNotasPaciente(${p.id}, '${p.nombre}')"><i class="fas fa-notes-medical"></i> Ver notas</button></td>
                        </tr>
                    `).join('');
                    }
                }
            }
        } catch(e) {}
        setLoading(false);
    } else if (termino.length === 0) {
        cargarPacientesDoctor();
    }
}

// ==================== DOCTOR - NOTAS MÉDICAS ====================
async function cargarPacientesParaNotas() {
    setLoading(true);
    try {
        // Solo MIS pacientes asignados (por citas atendidas)
        const res = await authFetch('/api/clientes/mis-pacientes');
        if (res.ok) {
            const pacientes = await res.json();
            const listaPacientesNota = document.getElementById('listaPacientesNota');
            
            if (listaPacientesNota) {
                if (pacientes.length === 0) {
                    listaPacientesNota.innerHTML = '<div class="alert alert-warning text-center">Aún no tienes pacientes asignados. Aparecerán cuando atiendas citas.</div>';
                } else {
                    listaPacientesNota.innerHTML = pacientes.map(p => `
                        <div class="paciente-item" 
                             data-id="${p.id}"
                             data-nombre="${p.nombre}"
                             style="cursor: pointer; 
                                    padding: 12px 15px; 
                                    margin-bottom: 8px; 
                                    border-radius: 10px; 
                                    background: white; 
                                    border: 1px solid #e0e0e0;
                                    transition: all 0.2s;
                                    display: flex;
                                    align-items: center;
                                    gap: 10px;">
                            <div style="width: 40px; height: 40px; background: #667eea; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white;">
                                <i class="fas fa-user"></i>
                            </div>
                            <div style="flex: 1;">
                                <strong style="font-size: 16px;">${p.nombre}</strong><br>
                                <small class="text-muted">${p.correo || 'Sin correo'} | ${p.telefono || 'Sin teléfono'}</small>
                            </div>
                            <div style="color: #667eea;">
                                <i class="fas fa-chevron-right"></i>
                            </div>
                        </div>
                    `).join('');
                    
                    document.querySelectorAll('.paciente-item').forEach(item => {
                        item.addEventListener('click', function() {
                            const id = parseInt(this.getAttribute('data-id'));
                            const nombre = this.getAttribute('data-nombre');
                            seleccionarPacienteNota(id, nombre);
                        });
                        item.addEventListener('mouseenter', function() {
                            this.style.backgroundColor = '#f0f2f5';
                            this.style.transform = 'translateX(5px)';
                        });
                        item.addEventListener('mouseleave', function() {
                            this.style.backgroundColor = 'white';
                            this.style.transform = 'translateX(0)';
                        });
                    });
                }
            }
        }
    } catch(e) { 
        console.error(e);
        const listaPacientesNota = document.getElementById('listaPacientesNota');
        if (listaPacientesNota) {
            listaPacientesNota.innerHTML = '<div class="alert alert-danger text-center">Error al cargar pacientes</div>';
        }
    }
    setLoading(false);
}

async function buscarPacienteNota() {
    const termino = this.value.trim();
    if (termino.length > 1) {
        setLoading(true);
        try {
            const res = await authFetch(`/api/clientes/buscar?nombre=${termino}`);
            if (res.ok) {
                const pacientes = await res.json();
                const listaPacientesNota = document.getElementById('listaPacientesNota');
                
                if (listaPacientesNota) {
                    if (pacientes.length === 0) {
                        listaPacientesNota.innerHTML = '<div class="alert alert-warning text-center">No se encontraron pacientes</div>';
                    } else {
                        listaPacientesNota.innerHTML = pacientes.map(p => `
                            <div class="paciente-item" 
                                 data-id="${p.id}"
                                 data-nombre="${p.nombre}"
                                 style="cursor: pointer; 
                                        padding: 12px 15px; 
                                        margin-bottom: 8px; 
                                        border-radius: 10px; 
                                        background: white; 
                                        border: 1px solid #e0e0e0;
                                        transition: all 0.2s;
                                        display: flex;
                                        align-items: center;
                                        gap: 10px;">
                                <div style="width: 40px; height: 40px; background: #667eea; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white;">
                                    <i class="fas fa-user"></i>
                                </div>
                                <div style="flex: 1;">
                                    <strong style="font-size: 16px;">${p.nombre}</strong><br>
                                    <small class="text-muted">${p.correo || 'Sin correo'} | ${p.telefono || 'Sin teléfono'}</small>
                                </div>
                                <div style="color: #667eea;">
                                    <i class="fas fa-chevron-right"></i>
                                </div>
                            </div>
                        `).join('');
                        
                        document.querySelectorAll('.paciente-item').forEach(item => {
                            item.addEventListener('click', function() {
                                const id = parseInt(this.getAttribute('data-id'));
                                const nombre = this.getAttribute('data-nombre');
                                seleccionarPacienteNota(id, nombre);
                            });
                            item.addEventListener('mouseenter', function() {
                                this.style.backgroundColor = '#f0f2f5';
                                this.style.transform = 'translateX(5px)';
                            });
                            item.addEventListener('mouseleave', function() {
                                this.style.backgroundColor = 'white';
                                this.style.transform = 'translateX(0)';
                            });
                        });
                    }
                }
            }
        } catch(e) {}
        setLoading(false);
    } else if (termino.length === 0) {
        cargarPacientesParaNotas();
    }
}

function seleccionarPacienteNota(id, nombre) {
    pacienteSeleccionadoIdNota = id;
    const notaPacienteId = document.getElementById('notaPacienteId');
    const pacienteSeleccionadoInfo = document.getElementById('pacienteSeleccionadoInfo');
    const pacienteSeleccionadoNombre = document.getElementById('pacienteSeleccionadoNombre');
    const btnGuardarNota = document.getElementById('btnGuardarNota');
    
    if (notaPacienteId) notaPacienteId.value = id;
    if (pacienteSeleccionadoNombre) pacienteSeleccionadoNombre.innerHTML = nombre;
    if (pacienteSeleccionadoInfo) pacienteSeleccionadoInfo.style.display = 'block';
    if (btnGuardarNota) btnGuardarNota.disabled = false;
    
    // Limpiar campos
    const notaTipo = document.getElementById('notaTipo');
    const notaContenido = document.getElementById('notaContenido');
    if (notaTipo) notaTipo.value = '';
    if (notaContenido) notaContenido.value = '';
    
    // Marcar visualmente el paciente seleccionado
    document.querySelectorAll('.paciente-item').forEach(item => {
        if (parseInt(item.getAttribute('data-id')) === id) {
            item.style.backgroundColor = '#e8f4fd';
            item.style.borderLeft = '4px solid #28a745';
        } else {
            item.style.backgroundColor = 'white';
            item.style.borderLeft = '1px solid #e0e0e0';
        }
    });
    
    // Cargar notas del paciente
    cargarNotasPaciente(id);
}

async function cargarNotasPaciente(pacienteId) {
    setLoading(true);
    try {
        const res = await authFetch(`/api/notas/doctor/paciente/${pacienteId}`);
        if (res.ok) {
            const notas = await res.json();
            const notasPacienteContainer = document.getElementById('notasPacienteContainer');
            
            if (notasPacienteContainer) {
                if (notas.length === 0) {
                    notasPacienteContainer.innerHTML = '<div class="alert alert-info text-center">No hay notas para este paciente.</div>';
                } else {
                    notasPacienteContainer.innerHTML = notas.map(n => `
                        <div class="nota-card" style="background: #f8f9fa; border-radius: 12px; padding: 15px; margin-bottom: 12px; border-left: 4px solid #667eea;">
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="badge ${n.tipo === 'DIAGNOSTICO' ? 'bg-primary' : n.tipo === 'RECOMENDACION' ? 'bg-success' : n.tipo === 'MEDICAMENTO' ? 'bg-warning' : 'bg-info'} text-white" style="padding: 5px 12px;">
                                    ${n.tipo === 'DIAGNOSTICO' ? '📋 Diagnóstico' : n.tipo === 'RECOMENDACION' ? '💡 Recomendación' : n.tipo === 'MEDICAMENTO' ? '💊 Medicamento' : '❤️ Consejo'}
                                </span>
                                <small class="text-muted"><i class="far fa-calendar-alt"></i> ${new Date(n.fechaCreacion).toLocaleString()}</small>
                            </div>
                            <p class="mt-2 mb-0" style="font-size: 14px;">${n.diagnostico || n.recomendaciones || n.medicamentos || ''}</p>
                            <hr class="my-2">
                            <small class="text-muted"><i class="fas fa-user-md"></i> Dr. ${n.doctor?.nombreCompleto || n.doctor?.username || 'Desconocido'}</small>
                        </div>
                    `).join('');
                }
            }
        }
    } catch(e) { 
        console.error(e);
        const notasPacienteContainer = document.getElementById('notasPacienteContainer');
        if (notasPacienteContainer) {
            notasPacienteContainer.innerHTML = '<div class="alert alert-danger text-center">Error al cargar notas</div>';
        }
    }
    setLoading(false);
}

async function guardarNotaMedica() {
    const notaPacienteId = document.getElementById('notaPacienteId');
    const notaTipo = document.getElementById('notaTipo');
    const notaContenido = document.getElementById('notaContenido');
    
    const pacienteId = notaPacienteId ? notaPacienteId.value : null;
    const tipo = notaTipo ? notaTipo.value : null;
    const contenido = notaContenido ? notaContenido.value : null;
    
    if (!pacienteId) {
        mostrarMensaje('❌ Primero selecciona un paciente de la lista', 'danger');
        return;
    }
    if (!tipo) {
        mostrarMensaje('❌ Selecciona un tipo de nota', 'danger');
        return;
    }
    if (!contenido || contenido.trim() === '') {
        mostrarMensaje('❌ Escribe el contenido de la nota', 'danger');
        return;
    }
    
    setLoading(true);
    try {
        let payload = { 
            pacienteId: parseInt(pacienteId), 
            tipo: tipo 
        };
        
        if (tipo === 'DIAGNOSTICO') payload.diagnostico = contenido;
        else if (tipo === 'RECOMENDACION') payload.recomendaciones = contenido;
        else if (tipo === 'MEDICAMENTO') payload.medicamentos = contenido;
        else payload.recomendaciones = contenido;
        
        const res = await authFetch('/api/notas', { method: 'POST', body: JSON.stringify(payload) });
        
        if (res.ok) {
            if (notaContenido) notaContenido.value = '';
            if (notaTipo) notaTipo.value = '';
            await cargarNotasPaciente(parseInt(pacienteId));
            mostrarMensaje('✅ Nota guardada correctamente', 'success');
        } else {
            const error = await res.text();
            mostrarMensaje('❌ Error: ' + error, 'danger');
        }
    } catch(e) {
        console.error(e);
        mostrarMensaje('❌ Error de conexión', 'danger');
    }
    setLoading(false);
}

function verNotasPaciente(id, nombre) {
    seleccionarPacienteNota(id, nombre);
    const notasTab = document.querySelector('[data-tab="notas"]');
    if (notasTab) notasTab.click();
}

// ==================== DOCTOR - CALENDARIO ====================
async function cargarCalendario() {
    const calendarEl = document.getElementById('calendar');
    if (!calendarEl) return;
    
    const res = await authFetch('/api/citas');
    const citas = await res.json();
    // /api/citas ya devuelve SOLO las citas de este doctor: se muestra TODA su agenda en el calendario.
    // Filtro opcional por estado desde la leyenda + colores por estado.
    const filtroEstado = document.getElementById('filtroCalendarioEstado')?.value || '';
    const citasFiltradas = filtroEstado ? citas.filter(c => c.estado === filtroEstado) : citas;
    const COLORES_ESTADO = { AGENDADA: '#0d6efd', REAGENDADA: '#6f42c1', EN_PROGRESO: '#ffc107', COMPLETADA: '#198754', CANCELADA: '#b6b6b6' };
    const events = citasFiltradas.map(c => ({
        id: c.id,
        title: `${c.cliente?.nombre || 'Paciente'} - ${c.servicio || 'Cita'}`,
        start: `${c.fecha}T${c.hora}`,
        backgroundColor: COLORES_ESTADO[c.estado] || '#6c757d',
        textColor: (c.estado === 'EN_PROGRESO' || c.estado === 'CANCELADA') ? '#212529' : '#fff',
        borderColor: c.estado === 'CANCELADA' ? '#999' : undefined,
        extendedProps: {
            estado: c.estado,
            cliente: c.cliente?.nombre,
            servicio: c.servicio,
            acompanante: c.acompananteNombre
        }
    }));
    
    if (calendar) calendar.destroy();
    calendar = new FullCalendar.Calendar(calendarEl, {
        locale: 'es',
        initialView: 'dayGridMonth',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
        },
        events: events,
        eventClick: function(info) {
            const c = info.event.extendedProps;
            mostrarMensaje(`Paciente: ${c.cliente}\nServicio: ${c.servicio}\nEstado: ${c.estado}${c.acompanante ? `\nAcompañante: ${c.acompanante}` : ''}`, 'info');
        },
        height: 600
    });
    calendar.render();
}