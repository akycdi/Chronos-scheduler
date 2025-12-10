import { useState, useEffect } from 'react'
import axios from 'axios'
import CreateJobModal from './CreateJobModal'
import './index.css'

function App() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [runningJobId, setRunningJobId] = useState(null)
  const [showCreateModal, setShowCreateModal] = useState(false)

  useEffect(() => {
    fetchJobs()
  }, [])

  const fetchJobs = async () => {
    try {
      const response = await axios.get('/api/jobs')
      setJobs(response.data.content || response.data)
      setLoading(false)
    } catch (err) {
      console.error(err)
      setError('Failed to fetch jobs')
      setLoading(false)
    }
  }

  const handleCreateJob = async (jobData) => {
    try {
      await axios.post('/api/jobs', jobData)
      setShowCreateModal(false)
      fetchJobs() // Refresh list
    } catch (err) {
      console.error(err)
      alert('Failed to create job: ' + (err.response?.data?.message || err.message))
    }
  }

  const handleRunJob = async (id) => {
    setRunningJobId(id)
    try {
      await axios.post(`/api/jobs/${id}/run`)
      setTimeout(fetchJobs, 1000)
    } catch (err) {
      alert('Failed to trigger job')
    } finally {
      setRunningJobId(null)
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED': return 'status-success'
      case 'FAILED': return 'status-error'
      case 'RUNNING': return 'status-running'
      case 'SCHEDULED': return 'status-info'
      default: return 'status-default'
    }
  }

  return (
    <div className="app-container">
      <header className="header">
        <h1>Chronos Scheduler</h1>
        <div className="header-actions">
           <button className="refresh-btn" onClick={fetchJobs}>Refresh</button>
           <button className="create-btn" onClick={() => setShowCreateModal(true)}>+ New Job</button>
        </div>
      </header>
      
      <main>
        {showCreateModal && (
          <CreateJobModal 
            onClose={() => setShowCreateModal(false)}
            onCreated={handleCreateJob}
          />
        )}

        {loading && <p>Loading jobs...</p>}
        {error && <p className="error-msg">{error}</p>}
        
        <div className="jobs-grid">
          {jobs.map(job => (
            <div key={job.id} className="job-card">
              <div className="job-header">
                <h3>{job.name}</h3>
                <span className={`status-badge ${getStatusColor(job.status)}`}>
                  {job.status}
                </span>
              </div>
              
              <div className="job-details">
                <p><strong>Type:</strong> {job.type}</p>
                <p><strong>Owner:</strong> {job.owner}</p>
                <p><strong>Last Run:</strong> {job.lastRunTime ? new Date(job.lastRunTime).toLocaleString() : 'Never'}</p>
                <p><strong>Next Run:</strong> {job.nextRunTime ? new Date(job.nextRunTime).toLocaleString() : 'Not scheduled'}</p>
              </div>

              <div className="job-actions">
                <button 
                  className="run-btn" 
                  disabled={runningJobId === job.id}
                  onClick={() => handleRunJob(job.id)}
                >
                  {runningJobId === job.id ? 'Starting...' : 'Run Now'}
                </button>
              </div>

              {job.recentRuns && job.recentRuns.length > 0 && 
               ['HTTP_REQUEST', 'SHELL_SCRIPT'].includes(job.type) && (
                <div className="job-output">
                  <h4>Last Outcome</h4>
                  <pre>{job.recentRuns[0].error || job.recentRuns[0].output || 'No output'}</pre>
                </div>
              )}
            </div>
          ))}
        </div>
      </main>
    </div>
  )
}

export default App
