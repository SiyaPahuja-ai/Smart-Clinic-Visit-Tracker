/**
 * app.js - Smart Clinic Web Dashboard Controller
 * Connects frontend UI to Java Backend REST API (ClinicManager & SQLite Database).
 * 100% real-time synchronized with Java backend (ArrayList, FIFO Queue, Streams).
 */

// Global state
let currentOverviewChart = null;
let currentStatusChart = null;
let allPatientsCache = [];

document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initDateDisplay();
    loadDashboard();

    // Auto-refresh queue and dashboard every 25 seconds for live clinic updates
    setInterval(() => {
        const activeSection = document.querySelector('.app-section.active-section');
        if (activeSection && activeSection.id === 'section-dashboard') {
            loadDashboard(true);
        } else if (activeSection && activeSection.id === 'section-queue') {
            loadQueue();
        }
    }, 25000);
});

// ========================================================
// NAVIGATION & UI INITIALIZATION
// ========================================================
function initNavigation() {
    const navLinks = document.querySelectorAll('.sidebar-menu .nav-link');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetSection = link.getAttribute('data-section');
            switchSection(targetSection);
        });
    });

    // Mobile sidebar toggle
    const toggleBtn = document.getElementById('sidebar-toggle');
    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            document.querySelector('.sidebar').classList.toggle('show');
        });
    }
}

function switchSection(sectionId) {
    // Update active nav link
    document.querySelectorAll('.sidebar-menu .nav-link').forEach(link => {
        if (link.getAttribute('data-section') === sectionId) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    // Show active section
    document.querySelectorAll('.app-section').forEach(section => {
        section.classList.remove('active-section');
    });
    const target = document.getElementById(`section-${sectionId}`);
    if (target) {
        target.classList.add('active-section');
    }

    // Update Topbar Title
    const titleMap = {
        'dashboard': 'Dashboard',
        'registration': 'Patient Registration',
        'patients': 'Patient Records',
        'appointments': 'Appointment Management',
        'queue': 'Token Queue (FIFO)',
        'history': 'Patient Visit History',
        'reports': 'Reports & Analytics',
        'settings': 'Settings'
    };
    document.getElementById('page-heading').innerText = titleMap[sectionId] || "Smart Clinic";

    // Lazy load section data
    if (sectionId === 'dashboard') loadDashboard();
    if (sectionId === 'patients') loadPatients();
    if (sectionId === 'appointments') loadAppointments();
    if (sectionId === 'queue') loadQueue();
    if (sectionId === 'history') loadHistoryPage();
    if (sectionId === 'reports') loadReports();

    // On mobile, close sidebar after clicking
    if (window.innerWidth <= 992) {
        document.querySelector('.sidebar').classList.remove('show');
    }
}

function initDateDisplay() {
    const options = { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' };
    const today = new Date().toLocaleDateString('en-US', options);
    const dateEl = document.getElementById('current-date');
    if (dateEl) dateEl.innerText = `📅 ${today}`;
}

// Toast helper
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast-msg ${type}`;
    toast.innerHTML = `<span>${type === 'success' ? '✓' : '⚠'}</span> <span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 3500);
}

// ========================================================
// 1. DASHBOARD CONTROLLER
// ========================================================
async function loadDashboard(silent = false) {
    try {
        const res = await fetch('/api/dashboard');
        if (!res.ok) throw new Error('Failed to load dashboard data');
        const data = await res.json();

        // Update 4 stat cards
        document.getElementById('stat-total-patients').innerText = data.totalPatients;
        document.getElementById('stat-today-appointments').innerText = data.todayAppointments;
        document.getElementById('stat-completed-consultations').innerText = data.completedConsultations;
        document.getElementById('stat-waiting-patients').innerText = data.waitingPatients;

        // Render Clean Charts
        renderOverviewChart(data.waitingPatients, data.completedConsultations, data.cancelledAppointments);
        renderStatusChart(data.waitingPatients, data.completedConsultations, data.cancelledAppointments);

        // Populate Recent Appointments Table
        renderRecentAppointments(data.recentAppointments);

    } catch (err) {
        if (!silent) showToast(err.message, 'error');
    }
}

function renderOverviewChart(scheduled, completed, cancelled) {
    const ctx = document.getElementById('chart-overview');
    if (!ctx) return;

    if (currentOverviewChart) {
        currentOverviewChart.destroy();
    }

    currentOverviewChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Waiting / Scheduled', 'Completed', 'Cancelled'],
            datasets: [{
                label: 'Appointments',
                data: [scheduled, completed, cancelled],
                backgroundColor: ['#0D6EFD', '#198754', '#DC3545'],
                borderRadius: 6,
                maxBarThickness: 45
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { precision: 0 }
                }
            }
        }
    });
}

function renderStatusChart(scheduled, completed, cancelled) {
    const ctx = document.getElementById('chart-status');
    if (!ctx) return;

    if (currentStatusChart) {
        currentStatusChart.destroy();
    }

    currentStatusChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Waiting', 'Completed', 'Cancelled'],
            datasets: [{
                data: [scheduled, completed, cancelled],
                backgroundColor: ['#0D6EFD', '#198754', '#DC3545'],
                borderWidth: 2,
                borderColor: '#FFFFFF'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom' }
            }
        }
    });
}

function escapeJs(str) {
    if (!str) return '';
    return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

function addDawaPreset(medicineText) {
    const textarea = document.getElementById('consult-prescription');
    if (!textarea) return;
    if (textarea.value.trim() === '') {
        textarea.value = medicineText;
    } else {
        textarea.value += ', ' + medicineText;
    }
    textarea.focus();
}

function openDawaModal(patientName, patientId, diagnosis, prescription, date) {
    document.getElementById('dawa-modal-patient').innerText = patientName || 'Patient';
    document.getElementById('dawa-modal-id').innerText = patientId || '---';
    document.getElementById('dawa-modal-date').innerText = date || new Date().toISOString().split('T')[0];
    document.getElementById('dawa-modal-diagnosis').innerText = diagnosis || 'General Health Consultation';
    document.getElementById('dawa-modal-prescription').innerHTML = (prescription && prescription !== 'null') ?
        `<div class="fw-bold text-success mb-1">Rx Prescribed Medicines (Dawai):</div><div class="fs-6 text-dark">${prescription}</div>` :
        '<span class="text-muted">Routine medical checkup. No special pharmaceutical medicines required.</span>';
    openModal('modal-view-dawa');
}

function renderRecentAppointments(list) {
    const tbody = document.getElementById('recent-appointments-tbody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No appointments recorded yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(a => `
        <tr>
            <td><strong>#${a.tokenNumber}</strong></td>
            <td><strong>${a.patientName}</strong></td>
            <td><span class="badge bg-light text-dark border">${a.patientId}</span></td>
            <td>${a.appointmentTime}</td>
            <td><span class="text-muted">Dr. Siya</span></td>
            <td>${getStatusBadge(a.status)}</td>
            <td>
                ${a.status === 'SCHEDULED' ?
                    `<button class="btn btn-sm btn-soft-green" onclick="openConsultationModal('${a.appointmentId}', '${escapeJs(a.patientName)}', ${a.tokenNumber})">Complete Visit</button>` :
                    (a.status === 'COMPLETED' ?
                        `<button class="btn btn-sm btn-outline-success me-1" onclick="openDawaModal('${escapeJs(a.patientName)}', '${a.patientId}', '${escapeJs(a.diagnosis || a.reason)}', '${escapeJs(a.prescription)}', '${a.appointmentDate}')">💊 Dawa</button>` :
                        `<button class="btn btn-sm btn-light border" onclick="viewPatientHistory('${a.patientId}')">History</button>`
                    )
                }
            </td>
        </tr>
    `).join('');
}

function getStatusBadge(status) {
    const s = (status || '').toUpperCase();
    if (s === 'SCHEDULED') return `<span class="status-badge badge-scheduled">Scheduled</span>`;
    if (s === 'COMPLETED') return `<span class="status-badge badge-completed">Completed</span>`;
    if (s === 'CANCELLED') return `<span class="status-badge badge-cancelled">Cancelled</span>`;
    return `<span class="status-badge badge-waiting">${s}</span>`;
}

// ========================================================
// 2 & 3. PATIENT RECORDS & REGISTRATION
// ========================================================
async function loadPatients(search = '') {
    try {
        const url = search ? `/api/patients?search=${encodeURIComponent(search)}` : '/api/patients';
        const res = await fetch(url);
        const patients = await res.json();
        allPatientsCache = patients;

        const tbody = document.getElementById('patients-table-tbody');
        if (!tbody) return;

        if (patients.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No patient records found.</td></tr>`;
            return;
        }

        tbody.innerHTML = patients.map(p => `
            <tr>
                <td><strong style="color: var(--primary-green);">${p.id}</strong></td>
                <td><strong>${p.name}</strong></td>
                <td>${p.age} yrs</td>
                <td><span class="badge bg-light text-dark border">${p.gender}</span></td>
                <td>${p.phone}</td>
                <td><span class="badge bg-success bg-opacity-75">${p.visitCount} visits</span></td>
                <td>
                    <button class="btn btn-sm btn-soft-green me-1" onclick="openBookAppointmentForPatient('${p.id}', '${escapeJs(p.name)}')">Book Apt</button>
                    <button class="btn btn-sm btn-light border me-1" onclick="openEditPatientModal('${p.id}', '${escapeJs(p.name)}', ${p.age}, '${p.gender}', '${p.phone}')">Edit</button>
                    <button class="btn btn-sm ${p.visitCount > 0 ? 'btn-outline-success' : 'btn-outline-secondary'}" onclick="viewPatientHistory('${p.id}')">${p.visitCount > 0 ? '💊 Dawa / History' : 'History'}</button>
                </td>
            </tr>
        `).join('');

    } catch (err) {
        showToast('Error loading patients: ' + err.message, 'error');
    }
}

function searchPatients() {
    const q = document.getElementById('patient-search-input').value.trim();
    loadPatients(q);
}

function openRegisterPatientModal() {
    document.getElementById('register-patient-form').reset();
    openModal('modal-register-patient');
}

async function handleRegisterPatient(e) {
    e.preventDefault();
    const name = document.getElementById('reg-patient-name').value.trim();
    const age = document.getElementById('reg-patient-age').value.trim();
    const gender = document.getElementById('reg-patient-gender').value;
    const phone = document.getElementById('reg-patient-phone').value.trim();

    try {
        const res = await fetch('/api/patients', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, age, gender, phone })
        });
        if (!res.ok) throw new Error('Registration failed');
        const created = await res.json();
        closeModal('modal-register-patient');
        showToast(`Patient ${created.name} registered with ID ${created.id}!`);
        loadPatients();
        loadDashboard(true);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handlePageRegisterPatient(e) {
    e.preventDefault();
    const name = document.getElementById('page-reg-name').value.trim();
    const age = document.getElementById('page-reg-age').value.trim();
    const gender = document.getElementById('page-reg-gender').value;
    const phone = document.getElementById('page-reg-phone').value.trim();

    try {
        const res = await fetch('/api/patients', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, age, gender, phone })
        });
        if (!res.ok) throw new Error('Registration failed');
        const created = await res.json();
        showToast(`Patient ${created.name} registered successfully with ID ${created.id}!`);
        document.getElementById('page-register-patient-form').reset();
        loadPatients();
        loadDashboard(true);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openEditPatientModal(id, name, age, gender, phone) {
    document.getElementById('edit-patient-id').value = id;
    document.getElementById('edit-patient-name').value = name;
    document.getElementById('edit-patient-age').value = age;
    document.getElementById('edit-patient-gender').value = gender;
    document.getElementById('edit-patient-phone').value = phone;
    openModal('modal-edit-patient');
}

async function handleEditPatient(e) {
    e.preventDefault();
    const id = document.getElementById('edit-patient-id').value;
    const name = document.getElementById('edit-patient-name').value.trim();
    const age = document.getElementById('edit-patient-age').value.trim();
    const gender = document.getElementById('edit-patient-gender').value;
    const phone = document.getElementById('edit-patient-phone').value.trim();

    try {
        const res = await fetch('/api/patients', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, name, age, gender, phone })
        });
        if (!res.ok) throw new Error('Update failed');
        closeModal('modal-edit-patient');
        showToast(`Patient ${id} details updated!`);
        loadPatients();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ========================================================
// 4. APPOINTMENT MANAGEMENT
// ========================================================
async function loadAppointments() {
    const status = document.getElementById('apt-filter-status') ? document.getElementById('apt-filter-status').value : 'ALL';
    const date = document.getElementById('apt-filter-date') ? document.getElementById('apt-filter-date').value : '';
    const sort = document.getElementById('apt-sort-by') ? document.getElementById('apt-sort-by').value : 'token';

    let url = `/api/appointments?status=${status}&sort=${sort}`;
    if (date) url += `&date=${date}`;

    try {
        const res = await fetch(url);
        const appointments = await res.json();

        const tbody = document.getElementById('appointments-table-tbody');
        if (!tbody) return;

        if (appointments.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No appointments found matching filters.</td></tr>`;
            return;
        }

        tbody.innerHTML = appointments.map(a => `
            <tr>
                <td><strong>${a.appointmentId}</strong></td>
                <td><strong style="color: var(--primary-green);">#${a.tokenNumber}</strong></td>
                <td>${a.appointmentDate}</td>
                <td>${a.appointmentTime}</td>
                <td><strong>${a.patientName}</strong> <span class="badge bg-light text-dark border">${a.patientId}</span></td>
                <td>
                    <div class="fw-semibold text-dark">${a.reason || 'General Consultation'}</div>
                    ${a.diagnosis ? `<div class="small text-success mt-1">🩺 <strong>Dx:</strong> ${a.diagnosis}</div>` : ''}
                </td>
                <td>${getStatusBadge(a.status)}</td>
                <td>
                    ${a.status === 'SCHEDULED' ? `
                        <button class="btn btn-sm btn-soft-green me-1" onclick="openConsultationModal('${a.appointmentId}', '${escapeJs(a.patientName)}', ${a.tokenNumber})">Complete</button>
                        <button class="btn btn-sm btn-light border me-1" onclick="openRescheduleModal('${a.appointmentId}', '${a.appointmentDate}', '${a.appointmentTime}')">Reschedule</button>
                        <button class="btn btn-sm btn-outline-danger" onclick="cancelAppointment('${a.appointmentId}', '${escapeJs(a.patientName)}')">Cancel</button>
                    ` : (a.status === 'COMPLETED' ? `
                        <button class="btn btn-sm btn-outline-success me-1" onclick="openDawaModal('${escapeJs(a.patientName)}', '${a.patientId}', '${escapeJs(a.diagnosis || a.reason)}', '${escapeJs(a.prescription)}', '${a.appointmentDate}')">💊 View Dawa</button>
                        <button class="btn btn-sm btn-light border" onclick="viewPatientHistory('${a.patientId}')">History</button>
                    ` : `<span class="badge bg-light text-secondary border">${a.status}</span>`)}
                </td>
            </tr>
        `).join('');

    } catch (err) {
        showToast('Error loading appointments: ' + err.message, 'error');
    }
}

async function openBookAppointmentModal() {
    try {
        const res = await fetch('/api/patients');
        const patients = await res.json();
        const select = document.getElementById('book-patient-select');
        select.innerHTML = `<option value="">-- Choose Patient --</option>` +
            patients.map(p => `<option value="${p.id}">${p.name} (ID: ${p.id}, Age: ${p.age})</option>`).join('');

        document.getElementById('book-apt-date').value = new Date().toISOString().split('T')[0];
        document.getElementById('book-apt-time').value = '10:00';
        document.getElementById('book-apt-reason').value = '';
        openModal('modal-book-appointment');
    } catch (err) {
        showToast('Failed to load patient list for booking', 'error');
    }
}

function openBookAppointmentForPatient(patientId, patientName) {
    openBookAppointmentModal().then(() => {
        const select = document.getElementById('book-patient-select');
        if (select) select.value = patientId;
    });
}

async function handleBookAppointment(e) {
    e.preventDefault();
    const patientId = document.getElementById('book-patient-select').value;
    const date = document.getElementById('book-apt-date').value;
    const time = document.getElementById('book-apt-time').value;
    const reason = document.getElementById('book-apt-reason').value.trim();

    if (!patientId) {
        showToast('Please select a patient', 'error');
        return;
    }

    try {
        const res = await fetch('/api/appointments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ patientId, date, time, reason })
        });
        if (!res.ok) throw new Error('Booking failed');
        const created = await res.json();
        closeModal('modal-book-appointment');
        showToast(`Appointment booked! Token #${created.tokenNumber} allocated (Added to FIFO Queue)`);
        loadAppointments();
        loadDashboard(true);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openRescheduleModal(appointmentId, currentDate, currentTime) {
    document.getElementById('resched-apt-id').value = appointmentId;
    document.getElementById('resched-date').value = currentDate;
    document.getElementById('resched-time').value = currentTime;
    openModal('modal-reschedule-appointment');
}

async function handleRescheduleAppointment(e) {
    e.preventDefault();
    const appointmentId = document.getElementById('resched-apt-id').value;
    const newDate = document.getElementById('resched-date').value;
    const newTime = document.getElementById('resched-time').value;

    try {
        const res = await fetch('/api/appointments/reschedule', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ appointmentId, newDate, newTime })
        });
        if (!res.ok) throw new Error('Reschedule failed');
        closeModal('modal-reschedule-appointment');
        showToast(`Appointment ${appointmentId} updated to ${newDate} at ${newTime}`);
        loadAppointments();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function cancelAppointment(appointmentId, patientName) {
    if (!confirm(`Cancel appointment for ${patientName} (${appointmentId})?`)) {
        return;
    }

    try {
        const res = await fetch('/api/appointments/cancel', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ appointmentId })
        });
        if (!res.ok) throw new Error('Cancellation failed');
        showToast(`Appointment ${appointmentId} cancelled and removed from queue.`);
        loadAppointments();
        loadDashboard(true);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ========================================================
// 5. TOKEN QUEUE MANAGEMENT (FIFO)
// ========================================================
async function loadQueue() {
    try {
        const res = await fetch('/api/queue');
        const data = await res.json();

        // Prominent Calling Card (Next Patient in Front of Queue)
        const next = data.nextPatient;
        const heroToken = document.getElementById('hero-token-number');
        const heroPatient = document.getElementById('hero-patient-name');
        const heroMeta = document.getElementById('hero-patient-meta');
        const heroCompleteBtn = document.getElementById('hero-complete-btn');

        if (next) {
            heroToken.innerText = `#${next.tokenNumber}`;
            heroPatient.innerText = next.patientName;
            heroMeta.innerText = `Patient ID: ${next.patientId} | Scheduled Time: ${next.appointmentTime} | Complaint: ${next.reason || 'General Examination'}`;
            if (heroCompleteBtn) {
                heroCompleteBtn.style.display = 'inline-block';
                heroCompleteBtn.onclick = () => openConsultationModal(next.appointmentId, next.patientName, next.tokenNumber);
            }
        } else {
            heroToken.innerText = 'NO QUEUE';
            heroPatient.innerText = 'All Patients Processed';
            heroMeta.innerText = 'Waiting line is currently clear.';
            if (heroCompleteBtn) heroCompleteBtn.style.display = 'none';
        }

        // Waiting List Table
        const tbody = document.getElementById('queue-table-tbody');
        if (!tbody) return;

        const list = data.waitingList || [];
        if (list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No patients currently waiting in queue.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map((a, idx) => `
            <tr>
                <td><span class="badge bg-light text-dark border">Pos ${idx + 1}</span></td>
                <td><strong style="color: var(--primary-green);">#${a.tokenNumber}</strong></td>
                <td><strong>${a.patientName}</strong></td>
                <td><span class="badge bg-light text-dark border">${a.patientId}</span></td>
                <td>${a.appointmentTime}</td>
                <td>${getStatusBadge(a.status)}</td>
            </tr>
        `).join('');

    } catch (err) {
        showToast('Error loading queue: ' + err.message, 'error');
    }
}

async function callNextPatient() {
    try {
        const res = await fetch('/api/queue/call-next', { method: 'POST' });
        const data = await res.json();

        if (data.success && data.calledPatient) {
            const p = data.calledPatient;
            showToast(`🔔 NOW CALLING: Token #${p.tokenNumber} (${p.patientName}) into Doctor's Cabin!`, 'success');
            loadQueue();
            loadDashboard(true);
        } else {
            showToast('Waiting queue is clear! No patients in queue.', 'error');
        }
    } catch (err) {
        showToast('Call next patient failed: ' + err.message, 'error');
    }
}

// ========================================================
// 6. CONSULTATION COMPLETION
// ========================================================
function openConsultationModal(appointmentId, patientName, tokenNumber) {
    document.getElementById('consult-apt-id').value = appointmentId;
    document.getElementById('consult-modal-info').innerText = `${patientName} (Token #${tokenNumber}, Apt ID: ${appointmentId})`;
    document.getElementById('consult-diagnosis').value = '';
    document.getElementById('consult-prescription').value = '';
    document.getElementById('consult-wait-time').value = '15';
    openModal('modal-complete-consultation');
}

async function handleCompleteConsultation(e) {
    e.preventDefault();
    const appointmentId = document.getElementById('consult-apt-id').value;
    const diagnosis = document.getElementById('consult-diagnosis').value.trim();
    const prescription = document.getElementById('consult-prescription').value.trim();
    const waitingTime = document.getElementById('consult-wait-time').value.trim();

    try {
        const res = await fetch('/api/consultation/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ appointmentId, diagnosis, prescription, waitingTime })
        });
        if (!res.ok) throw new Error('Failed to complete consultation');
        const vr = await res.json();

        closeModal('modal-complete-consultation');
        showToast(`Consultation completed! Visit Record ${vr.recordId} saved to patient history.`);

        // Refresh views
        loadDashboard(true);
        loadAppointments();
        loadQueue();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ========================================================
// 7. PATIENT VISIT HISTORY
// ========================================================
async function loadHistoryPage() {
    try {
        const res = await fetch('/api/patients');
        const patients = await res.json();
        const select = document.getElementById('history-patient-select');
        select.innerHTML = `<option value="">-- Choose Registered Patient --</option>` +
            patients.map(p => `<option value="${p.id}">${p.name} (ID: ${p.id}, Total Visits: ${p.visitCount})</option>`).join('');
    } catch (err) {
        showToast('Failed to load patient dropdown for history', 'error');
    }
}

function handleHistoryPatientSelect() {
    const pid = document.getElementById('history-patient-select').value;
    if (pid) {
        viewPatientHistory(pid);
    } else {
        document.getElementById('history-timeline-container').innerHTML = '';
        document.getElementById('history-patient-card').style.display = 'none';
    }
}

async function viewPatientHistory(patientId) {
    switchSection('history');
    document.getElementById('history-patient-select').value = patientId;

    try {
        // Fetch patient profile
        const pRes = await fetch(`/api/patients?search=${encodeURIComponent(patientId)}`);
        const patients = await pRes.json();
        const patient = patients.find(p => p.id === patientId) || patients[0];

        if (patient) {
            document.getElementById('history-patient-card').style.display = 'block';
            document.getElementById('hp-name').innerText = patient.name;
            document.getElementById('hp-id').innerText = patient.id;
            document.getElementById('hp-meta').innerText = `Age: ${patient.age} yrs | Gender: ${patient.gender} | Phone: ${patient.phone}`;
            document.getElementById('hp-visits').innerText = `${patient.visitCount} Consultations`;
        }

        // Fetch visit records
        const vRes = await fetch(`/api/visits?patientId=${encodeURIComponent(patientId)}`);
        const visits = await vRes.json();
        const container = document.getElementById('history-timeline-container');

        if (!visits || visits.length === 0) {
            container.innerHTML = `<div class="text-center py-5 text-muted">No past clinical consultations recorded for this patient yet. Complete a consultation to record diagnosis and prescriptions.</div>`;
            return;
        }

        container.innerHTML = `
            <div class="timeline">
                ${visits.map(v => `
                    <div class="timeline-item">
                        <div class="timeline-dot"></div>
                        <div class="timeline-content">
                            <div class="d-flex justify-content-between align-items-center mb-2 flex-wrap gap-2">
                                <h6 class="mb-0 fw-bold" style="color: var(--primary-green);">Medical Record #${v.recordId} (${v.appointmentId})</h6>
                                <span class="badge bg-light text-dark border">${v.visitDate} • Wait: ${v.waitingTimeMinutes} mins</span>
                            </div>
                            <div class="mb-2">
                                <strong class="text-dark">Medical Problem / Diagnosis:</strong>
                                <p class="mb-1 text-secondary fw-semibold">${v.diagnosis || 'Standard Health Checkup'}</p>
                            </div>
                            <div class="p-3 rounded mt-2" style="background-color: #F8FBF9; border: 1px solid var(--soft-green); border-left: 4px solid var(--primary-green);">
                                <div class="d-flex justify-content-between align-items-center mb-1">
                                    <strong style="color: var(--dark-green);">💊 Prescribed Dawai & Dosage (Rx):</strong>
                                    <button class="btn btn-xs btn-outline-success py-0 px-2" style="font-size: 11px;" onclick="openDawaModal('${escapeJs(patient.name)}', '${patient.id}', '${escapeJs(v.diagnosis)}', '${escapeJs(v.prescription)}', '${v.visitDate}')">Print Rx</button>
                                </div>
                                <p class="mb-0 fw-bold fs-6" style="color: var(--primary-green);">${v.prescription || 'No medicines required'}</p>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;

    } catch (err) {
        showToast('Error loading visit history: ' + err.message, 'error');
    }
}

// ========================================================
// 8. REPORTS & ANALYTICS (JAVA STREAMS)
// ========================================================
async function loadReports() {
    try {
        const res = await fetch('/api/reports');
        const r = await res.json();

        document.getElementById('report-total-patients').innerText = r.totalPatients;
        document.getElementById('report-total-appointments').innerText = r.totalAppointments;
        document.getElementById('report-today-appointments').innerText = r.todayAppointments;
        document.getElementById('report-avg-wait').innerText = `${r.avgWaitingTime} mins`;

        const total = r.totalAppointments || 1;
        const schedPct = ((r.scheduledCount / total) * 100).toFixed(1);
        const compPct = ((r.completedCount / total) * 100).toFixed(1);
        const cancPct = ((r.cancelledCount / total) * 100).toFixed(1);

        document.getElementById('report-scheduled-count').innerText = `${r.scheduledCount} (${schedPct}%)`;
        document.getElementById('report-completed-count').innerText = `${r.completedCount} (${compPct}%)`;
        document.getElementById('report-cancelled-count').innerText = `${r.cancelledCount} (${cancPct}%)`;

        document.getElementById('report-generated-timestamp').innerText = new Date().toLocaleString();

    } catch (err) {
        showToast('Error loading report analytics: ' + err.message, 'error');
    }
}

function printReports() {
    window.print();
}

// ========================================================
// MODAL CONTROLS
// ========================================================
function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('active');
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('active');
}

window.onclick = function(e) {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
    }
};
