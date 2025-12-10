import { useState } from 'react'

const JOB_TYPES = [
  'HTTP_REQUEST',
  'SHELL_SCRIPT',
  'JAVA_CLASS',
  'PYTHON_SCRIPT',
  // 'CUSTOM' 
]

export default function CreateJobModal({ onClose, onCreated }) {
  const [formData, setFormData] = useState({
    name: '',
    owner: '',
    type: 'HTTP_REQUEST',
    description: '',
    schedule: '',
    isRecurring: false,
    maxRetries: 3,
  })

  // Dynamic fields state
  const [jobDetails, setJobDetails] = useState({
    url: '',
    method: 'GET',
    timeout: 60,
    script: '',
    className: '',
    scriptPath: ''
  })

  // Advanced config
  const [useAdvanced, setUseAdvanced] = useState(false)
  const [retryDelay, setRetryDelay] = useState(5)

  const handleSubmit = async (e) => {
    e.preventDefault()

    // Construct jobData JSON based on type
    const jobDataObj = {}
    if (formData.type === 'HTTP_REQUEST') {
      jobDataObj.url = jobDetails.url
      jobDataObj.method = jobDetails.method
      jobDataObj.timeout = jobDetails.timeout
    } else if (formData.type === 'SHELL_SCRIPT') {
      jobDataObj.script = jobDetails.script
    } else if (formData.type === 'JAVA_CLASS') {
      jobDataObj.className = jobDetails.className
    } else if (formData.type === 'PYTHON_SCRIPT') {
      jobDataObj.script = jobDetails.script
      jobDataObj.scriptPath = jobDetails.scriptPath
    }

    const payload = {
      ...formData,
      jobData: JSON.stringify(jobDataObj),
      config: JSON.stringify({ retryDelaySeconds: parseInt(retryDelay) })
    }

    // Convert maxRetries to number
    payload.maxRetries = parseInt(payload.maxRetries)

    onCreated(payload)
  }

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div className="modal-header">
          <h2>Create New Job</h2>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="create-job-form">
          {/* Basic Info */}
          <div className="form-group">
            <label>Job Name</label>
            <input 
              required 
              value={formData.name}
              onChange={e => setFormData({...formData, name: e.target.value})}
              placeholder="e.g., Daily Cleanup"
            />
          </div>

          <div className="form-group">
            <label>Owner</label>
            <input 
              required 
              value={formData.owner}
              onChange={e => setFormData({...formData, owner: e.target.value})}
              placeholder="e.g., dev-team"
            />
          </div>

          <div className="form-group">
            <label>Type</label>
            <select 
              value={formData.type}
              onChange={e => setFormData({...formData, type: e.target.value})}
            >
              {JOB_TYPES.map(t => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>

          {/* Dynamic Fields */}
          {formData.type === 'HTTP_REQUEST' && (
            <div className="dynamic-section">
               <div className="form-group">
                <label>URL</label>
                <input 
                  required 
                  type="url"
                  value={jobDetails.url}
                  onChange={e => setJobDetails({...jobDetails, url: e.target.value})}
                  placeholder="https://api.example.com/tasks"
                />
              </div>
              <div className="form-row">
                <div className="form-group half">
                  <label>Method</label>
                  <select 
                    value={jobDetails.method}
                    onChange={e => setJobDetails({...jobDetails, method: e.target.value})}
                  >
                    <option>GET</option>
                    <option>POST</option>
                    <option>PUT</option>
                    <option>DELETE</option>
                  </select>
                </div>
                <div className="form-group half">
                  <label>Timeout (sec)</label>
                  <input 
                    type="number"
                    value={jobDetails.timeout}
                    onChange={e => setJobDetails({...jobDetails, timeout: parseInt(e.target.value)})}
                  />
                </div>
              </div>
            </div>
          )}

          {formData.type === 'SHELL_SCRIPT' && (
             <div className="dynamic-section">
               <div className="form-group">
                <label>Script Command</label>
                <textarea 
                  required 
                  rows={3}
                  value={jobDetails.script}
                  onChange={e => setJobDetails({...jobDetails, script: e.target.value})}
                  placeholder="echo 'Hello World'"
                  style={{fontFamily: 'monospace'}}
                />
              </div>
             </div>
          )}

          {formData.type === 'JAVA_CLASS' && (
             <div className="dynamic-section">
               <div className="form-group">
                <label>Class Name</label>
                <input 
                  required 
                  value={jobDetails.className}
                  onChange={e => setJobDetails({...jobDetails, className: e.target.value})}
                  placeholder="com.example.MyJob"
                />
              </div>
             </div>
          )}

          {formData.type === 'PYTHON_SCRIPT' && (
             <div className="dynamic-section">
               <div className="form-group">
                <label>Script Path (Optional if script provided)</label>
                <input 
                  value={jobDetails.scriptPath}
                  onChange={e => setJobDetails({...jobDetails, scriptPath: e.target.value})}
                  placeholder="/path/to/script.py"
                />
              </div>
              <div className="form-group">
                <label>Or Inline Script</label>
                <textarea 
                  rows={3}
                  value={jobDetails.script}
                  onChange={e => setJobDetails({...jobDetails, script: e.target.value})}
                  placeholder="print('Hello from Python')"
                  style={{fontFamily: 'monospace'}}
                />
              </div>
             </div>
          )}

          {/* Scheduling */}
          <div className="form-section-title">Scheduling</div>
          <div className="form-row">
            <div className="form-group checkbox-group">
              <label>
                <input 
                  type="checkbox"
                  checked={formData.isRecurring}
                  onChange={e => setFormData({...formData, isRecurring: e.target.checked})}
                />
                Recurring Job
              </label>
            </div>
          </div>

          <div className="form-group">
            <label>{formData.isRecurring ? 'Cron Expression' : 'Run At (ISO Date)'}</label>
            <input 
              value={formData.schedule}
              onChange={e => setFormData({...formData, schedule: e.target.value})}
              placeholder={formData.isRecurring ? "0 0 12 * * ?" : "2023-12-31T23:59:00"}
            />
            {formData.isRecurring && <small className="helper-text">Format: src min hr day month day-of-week</small>}
          </div>

          {/* Advanced Toggle */}
          <div className="advanced-toggle" onClick={() => setUseAdvanced(!useAdvanced)}>
            {useAdvanced ? '▼ Hide Advanced Options' : '▶ Show Advanced Options'}
          </div>

          {useAdvanced && (
            <div className="advanced-section">
              <div className="form-row">
                <div className="form-group half">
                  <label>Max Retries</label>
                  <input 
                    type="number"
                    value={formData.maxRetries}
                    onChange={e => setFormData({...formData, maxRetries: e.target.value})}
                  />
                </div>
                <div className="form-group half">
                  <label>Retry Delay (sec)</label>
                  <input 
                    type="number"
                    value={retryDelay}
                    onChange={e => setRetryDelay(e.target.value)}
                  />
                </div>
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea 
                  value={formData.description}
                  onChange={e => setFormData({...formData, description: e.target.value})}
                />
              </div>
            </div>
          )}

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn-primary">Create Job</button>
          </div>
        </form>
      </div>
    </div>
  )
}
