import React, { useEffect, useMemo, useState } from 'react';
import { FiActivity, FiAlertTriangle, FiCheckCircle, FiDatabase, FiRefreshCw } from 'react-icons/fi';
import BPMNViewer from './BPMNViewer';
import './App.css';

const defaultForm = () => ({
  externalId: `TEST-${Math.floor(Math.random() * 10000)}`,
  description: 'Simulação de atendimento técnico',
  clientType: 'GOLD',
  origin: 'APP',
  forceSagaFailure: false
});

function App() {
  const [formData, setFormData] = useState(defaultForm());
  const [tickets, setTickets] = useState([]);
  const [selectedExternalId, setSelectedExternalId] = useState('');
  const [history, setHistory] = useState([]);
  const [currentStatus, setCurrentStatus] = useState('UNKNOWN');
  const [loading, setLoading] = useState(false);

  const selectedTimelineId = useMemo(() => selectedExternalId || formData.externalId, [selectedExternalId, formData.externalId]);

  const fetchTickets = async () => {
    setLoading(true);
    try {
      const res = await fetch('/api/v1/process/tickets');
      const data = await res.json();
      setTickets(Array.isArray(data) ? data.reverse() : []);
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async (externalId) => {
    if (!externalId) return;
    const [historyRes, statusRes] = await Promise.all([
      fetch(`/api/v1/process/${externalId}/history`),
      fetch(`/api/v1/process/${externalId}/status`)
    ]);
    const historyData = await historyRes.json();
    const statusData = await statusRes.json();
    setHistory(Array.isArray(historyData) ? historyData : []);
    setCurrentStatus(statusData.currentStatus || 'UNKNOWN');
  };

  useEffect(() => {
    fetchTickets();
  }, []);

  useEffect(() => {
    if (!selectedTimelineId) return;
    fetchHistory(selectedTimelineId);
    const interval = setInterval(() => fetchHistory(selectedTimelineId), 3000);
    return () => clearInterval(interval);
  }, [selectedTimelineId]);

  const triggerSimulation = async (forceSagaFailure) => {
    const payload = { ...formData, forceSagaFailure };
    const res = await fetch('/api/v1/process/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    setSelectedExternalId(data.externalId);
    setFormData((prev) => ({ ...prev, externalId: data.externalId }));
    setTimeout(fetchTickets, 2000);
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <h1>⚡ ThunderStruck Test Panel (Sucesso/Falha + Histórico)</h1>
      </header>

      <main className="content-single-page">
        <div className="simulator-container">
          <div className="section-header"><FiActivity /><h2>Painel de Simulação</h2></div>
          <BPMNViewer diagramUrl="/thunderstruck-process-usertasks-v3.bpmn" />

          <div className="form-grid">
            <div className="field"><label>External ID</label><input value={formData.externalId} onChange={(e) => setFormData({ ...formData, externalId: e.target.value })} /></div>
            <div className="field"><label>Descrição</label><input value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} /></div>
            <div className="field"><label>Perfil (TMF/DMN)</label><select value={formData.clientType} onChange={(e) => setFormData({ ...formData, clientType: e.target.value })}><option value="GOLD">GOLD</option><option value="STANDARD">STANDARD</option></select></div>
            <div className="field"><label>Origem</label><select value={formData.origin} onChange={(e) => setFormData({ ...formData, origin: e.target.value })}><option value="APP">APP</option><option value="WEB">WEB</option><option value="ADMIN">ADMIN</option></select></div>
          </div>

          <div className="button-row">
            <button className="btn-primary success" onClick={() => triggerSimulation(false)}><FiCheckCircle /> Simular Sucesso</button>
            <button className="btn-primary danger" onClick={() => triggerSimulation(true)}><FiAlertTriangle /> Simular Falha com Rollback</button>
          </div>

          <div className="status-panel">
            <strong>Status Atual:</strong> <span>{currentStatus}</span>
            <button className="btn-refresh" onClick={() => fetchHistory(selectedTimelineId)}><FiRefreshCw /></button>
          </div>

          <div className="timeline">
            <h3>Histórico Detalhado ({selectedTimelineId || 'selecione/execute um processo'})</h3>
            {history.map((item) => (
              <div key={item.id} className="timeline-item">
                <div><strong>{item.stage}</strong> • {item.source}</div>
                <div>{item.previousStatus || '-'} → <strong>{item.currentStatus}</strong></div>
                <div>{item.details}</div>
                <small>{item.createdAt}</small>
              </div>
            ))}
          </div>

          <div className="tmf-section">
            <div className="section-header" style={{ color: '#a6e3a1' }}>
              <FiDatabase /><h2>Tickets TMF621</h2>
              <button className="btn-refresh" onClick={fetchTickets} disabled={loading}><FiRefreshCw className={loading ? 'spin' : ''} /></button>
            </div>
            <div className="ticket-table-wrapper">
              <table className="ticket-table">
                <thead>
                  <tr><th>ID</th><th>External ID</th><th>Status</th><th>Severity</th><th>Origem</th></tr>
                </thead>
                <tbody>
                  {tickets.map((t) => (
                    <tr key={t.id} onClick={() => setSelectedExternalId(t.externalId)}>
                      <td>{t.id}</td><td>{t.externalId}</td><td>{t.status}</td><td>{t.severity}</td><td>{t.relatedPartyId}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
