document.getElementById('transferForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const cuentaOrigen = document.getElementById('cuentaOrigen').value;
    const cuentaDestino = document.getElementById('cuentaDestino').value;
    const monto = parseFloat(document.getElementById('monto').value);
    const mensajeResult = document.getElementById('mensajeResult');
    const btnTransferir = document.getElementById('btnTransferir');

    // Validación client-side
    if(cuentaOrigen === cuentaDestino) {
        mostrarMensaje('La cuenta origen y destino no pueden ser iguales', 'error');
        return;
    }

    const payload = {
        cuentaOrigen: cuentaOrigen,
        cuentaDestino: cuentaDestino,
        monto: monto
    };

    btnTransferir.disabled = true;
    btnTransferir.innerText = "Procesando SAGA...";
    mensajeResult.className = 'hidden';

    try {
        const response = await fetch('/api/transferencias', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const resultText = await response.text();

        if (response.ok) {
            mostrarMensaje(resultText, 'success');
            agregarAHistorial(`Exitosa: $${monto} de ${cuentaOrigen} a ${cuentaDestino}`);
        } else {
            mostrarMensaje(resultText, 'error');
            agregarAHistorial(`Fallida/Revertida: $${monto} de ${cuentaOrigen} a ${cuentaDestino} - ${resultText}`);
        }
    } catch (error) {
        mostrarMensaje('Error de conexión con el servidor', 'error');
    } finally {
        btnTransferir.disabled = false;
        btnTransferir.innerText = "Ejecutar Transferencia SAGA";
    }
});

function mostrarMensaje(texto, tipo) {
    const box = document.getElementById('mensajeResult');
    box.innerText = texto;
    box.className = tipo === 'success' ? 'alert alert-success' : 'alert alert-danger';
}

function agregarAHistorial(texto) {
    const ul = document.getElementById('historialList');
    const li = document.createElement('li');
    li.innerText = `${new Date().toLocaleTimeString()} - ${texto}`;
    ul.prepend(li);
}