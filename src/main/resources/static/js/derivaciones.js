// ==================== DERIVACIONES ENTRE DEPARTAMENTOS ====================
// Módulo compartido por enfermería, psicología, psiquiatría y trabajo social:
// agrega un botón "Derivar" en las tablas de pacientes y envía la solicitud al
// jefe del departamento destino, que la aprueba creando asignación y cita.

const DEPARTAMENTOS_CLINICOS = [
    { valor: 'PSICOLOGIA', nombre: 'Psicología' },
    { valor: 'PSIQUIATRIA', nombre: 'Psiquiatría' },
    { valor: 'ENFERMERIA', nombre: 'Enfermería' },
    { valor: 'TRABAJO_SOCIAL', nombre: 'Trabajo Social' }
];

function asegurarModalDerivar() {
    if (document.getElementById('modalDerivar')) return;
    const modal = document.createElement('div');
    modal.innerHTML = `
    <div class="modal fade" id="modalDerivar" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fas fa-share"></i> Derivar paciente</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p class="mb-3">Paciente: <strong id="derivPacienteNombre"></strong></p>
                    <input type="hidden" id="derivPacienteId">
                    <div class="mb-3">
                        <label class="form-label">Departamento destino *</label>
                        <select id="derivDestino" class="form-select"></select>
                    </div>
                    <div class="mb-0">
                        <label class="form-label">Motivo de la derivación *</label>
                        <textarea id="derivMotivo" class="form-control" rows="3"
                            placeholder="Ej: requiere valoración psiquiátrica por sintomatología persistente"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-primary" onclick="confirmarDerivar()">
                        <i class="fas fa-paper-plane"></i> Enviar solicitud
                    </button>
                </div>
            </div>
        </div>
    </div>`;
    document.body.appendChild(modal.firstElementChild);
    document.getElementById('derivDestino').innerHTML = DEPARTAMENTOS_CLINICOS
        .map(d => `<option value="${d.valor}">${d.nombre}</option>`).join('');
}

function abrirModalDerivar(pacienteId, nombrePaciente) {
    asegurarModalDerivar();
    document.getElementById('derivPacienteId').value = pacienteId;
    document.getElementById('derivPacienteNombre').textContent = nombrePaciente || '';
    document.getElementById('derivMotivo').value = '';
    new bootstrap.Modal(document.getElementById('modalDerivar')).show();
}

async function confirmarDerivar() {
    const pacienteId = parseInt(document.getElementById('derivPacienteId').value, 10);
    const departamentoDestino = document.getElementById('derivDestino').value;
    const motivo = document.getElementById('derivMotivo').value.trim();

    if (!pacienteId || !departamentoDestino) return;
    if (!motivo) {
        mostrarMensaje('Explica el motivo de la derivación', 'danger');
        return;
    }

    if (typeof setLoading === 'function') setLoading(true);
    try {
        const res = await authFetch('/api/derivaciones', {
            method: 'POST',
            body: JSON.stringify({ clienteId: pacienteId, departamentoDestino, motivo })
        });
        if (res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('modalDerivar'))?.hide();
            mostrarMensaje('✅ Solicitud enviada al jefe de ' + departamentoDestino);
        } else {
            const texto = await res.text();
            mostrarMensaje('❌ ' + (texto.slice(0, 160) || 'No se pudo enviar la solicitud'), 'danger');
        }
    } catch (e) {
        mostrarMensaje('Error de conexión', 'danger');
    }
    if (typeof setLoading === 'function') setLoading(false);
}
