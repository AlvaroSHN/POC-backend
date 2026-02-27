import React, { useEffect, useMemo, useState } from 'react';
import { FiActivity, FiAlertTriangle, FiCheckCircle, FiDatabase, FiRefreshCw } from 'react-icons/fi';
import BPMNViewer from './BPMNViewer';
import './App.css';

const failOptions = ['NONE', 'PROTOCOL', 'INTERACTION', 'INTERACTION_ITEM', 'CASE', 'NOTIFICATION'];

const defaultForm = () => ({
  externalId: `CASE-${Math.floor(Math.random() * 10000)}`,
  description: 'Cliente abriu reclamação de indisponibilidade',
  clientType: 'GOLD',
  origin: 'APP',
  simulateFailAt: 'NONE'
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

  useEffect(() => { fetchTickets(); }, []);

  useEffect(() => {
    if (!selectedTimelineId) return;
    fetchHistory(selectedTimelineId);
    const interval = setInterval(() => fetchHistory(selectedTimelineId), 2500);
    return () => clearInterval(interval);
  }, [selectedTimelineId]);

  const triggerSimulation = async () => {
    const res = await fetch('/api/v1/process/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });
    const data = await res.json();
    setSelectedExternalId(data.externalId);
    setTimeout(fetchTickets, 1800);
  };

  return (
    <div className="app-container">
      <header className="app-header"><h1>⚡ Thunderstruck Full Capability Test Panel</h1></header>
      <main className="content-single-page">
        <div className="simulator-container">
          <div className="section-header"><FiActivity /><h2>Simulação End2End (SAGA + DMN + UserTask)</h2></div>
          <BPMNViewer diagramUrl="/thunderstruck-camunda7-saga.bpmn" />

          <div className="form-grid">
            <div className="field"><label>External ID</label><input value={formData.externalId} onChange={(e) => setFormData({ ...formData, externalId: e.target.value })} /></div>
            <div className="field"><label>Descrição da reclamação</label><input value={formData.description} onChange={(e) => setFormData({ ...formData, description: e.target.value })} /></div>
            <div className="field"><label>Tipo cliente</label><select value={formData.clientType} onChange={(e) => setFormData({ ...formData, clientType: e.target.value })}><option value="GOLD">GOLD</option><option value="STANDARD">STANDARD</option></select></div>
            <div className="field"><label>Origem</label><select value={formData.origin} onChange={(e) => setFormData({ ...formData, origin: e.target.value })}><option value="APP">APP</option><option value="WEB">WEB</option><option value="ADMIN">ADMIN</option></select></div>
            <div className="field" style={{ gridColumn: 'span 2' }}><label>Falha simulada</label><select value={formData.simulateFailAt} onChange={(e) => setFormData({ ...formData, simulateFailAt: e.target.value })}>{failOptions.map(x => <option key={x} value={x}>{x}</option>)}</select></div>
          </div>

          <div className="button-row">
            <button className="btn-primary success" onClick={triggerSimulation}><FiCheckCircle /> Executar simulação</button>
            <button className="btn-primary danger" onClick={() => setFormData(defaultForm())}><FiAlertTriangle /> Novo cenário</button>
          </div>

          <div className="status-panel"><strong>Status Atual:</strong> <span>{currentStatus}</span><button className="btn-refresh" onClick={() => fetchHistory(selectedTimelineId)}><FiRefreshCw /></button></div>

          <div className="timeline">
            <h3>Transformação por caixinha BPMN ({selectedTimelineId || 'execute um cenário'})</h3>
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
            <div className="section-header" style={{ color: '#a6e3a1' }}><FiDatabase /><h2>TroubleTicket (TMF621)</h2><button className="btn-refresh" onClick={fetchTickets} disabled={loading}><FiRefreshCw className={loading ? 'spin' : ''} /></button></div>
            <div className="ticket-table-wrapper">
              <table className="ticket-table">
                <thead><tr><th>ID</th><th>External ID</th><th>Status</th><th>Severity</th><th>Origem</th></tr></thead>
                <tbody>{tickets.map((t) => <tr key={t.id} onClick={() => setSelectedExternalId(t.externalId)}><td>{t.id}</td><td>{t.externalId}</td><td>{t.status}</td><td>{t.severity}</td><td>{t.relatedPartyId}</td></tr>)}</tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
