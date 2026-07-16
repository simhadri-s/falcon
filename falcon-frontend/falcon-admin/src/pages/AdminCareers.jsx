import { useEffect, useState } from 'react';
import api from '../services/api';
import { X } from 'lucide-react';

export default function AdminCareers() {
  // Data State
  const [jobs, setJobs] = useState([]);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Job Modal State
  const [isJobModalOpen, setIsJobModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingJob, setEditingJob] = useState(null); // Track the job being edited
  const [jobForm, setJobForm] = useState({
    title: '', department: '', location: '', type: 'Full-time', active: true
  });

  // App Modal State (For viewing Cover Letters)
  const [selectedApp, setSelectedApp] = useState(null);

  // Filter State
  const [selectedJobFilter, setSelectedJobFilter] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [jobsRes, appsRes] = await Promise.all([
        api.get('/api/jobs'),
        api.get('/api/applications')
      ]);
      setJobs(jobsRes.data.data || jobsRes.data || []);
      setApplications(appsRes.data.data || appsRes.data || []);
    } catch (err) {
      console.error("Error fetching careers data:", err);
      setError('Failed to load jobs or applications.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // --- Job Form Handlers ---
  const handleJobInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setJobForm({ ...jobForm, [name]: type === 'checkbox' ? checked : value });
  };

  const handleAddJobClick = () => {
    setEditingJob(null);
    setJobForm({ title: '', department: '', location: '', type: 'Full-time', active: true });
    setIsJobModalOpen(true);
  };

  const handleEditJobClick = (job) => {
    setEditingJob(job);
    setJobForm({
      title: job.title || '',
      department: job.department || '',
      location: job.location || '',
      type: job.type || 'Full-time',
      active: job.active !== undefined ? job.active : true
    });
    setIsJobModalOpen(true);
  };

  const handleDeleteJob = async (id) => {
    if (window.confirm("Are you sure you want to delete this job posting?")) {
      try {
        await api.delete(`/api/jobs/${id}`);
        setJobs(jobs.filter(j => (j.id || j._id) !== id));
      } catch (err) {
        console.error("Delete job failed", err);
        alert("Failed to delete job.");
      }
    }
  };

  const handleDeleteApplication = async (id) => {
    if (window.confirm("Are you sure you want to delete this job application?")) {
      try {
        await api.delete(`/api/applications/${id}`);
        setApplications(applications.filter(app => (app.id || app._id) !== id));
      } catch (err) {
        console.error("Delete job application failed", err);
        alert("Failed to delete job application.");
      }
    }
  };

  const handleJobSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      if (editingJob) {
        // Edit Existing Job
        const jobId = editingJob.id || editingJob._id;
        await api.put(`/api/jobs/${jobId}`, jobForm);
      } else {
        // Create New Job
        await api.post('/api/jobs', jobForm);
      }
      
      fetchData(); // Refresh the data to show changes
      setIsJobModalOpen(false);
      setEditingJob(null); // Clear edit state
    } catch (err) {
      console.error("Submit job failed", err);
      alert(editingJob ? "Failed to update job." : "Failed to create job.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const getJobTitle = (jobId) => {
    const job = jobs.find(j => (j.id || j._id) === jobId);
    return job ? job.title : 'Unknown Role';
  };

  if (loading) return <div className="p-6 text-center text-[0.85rem] text-gray-500">Loading careers data...</div>;

  const displayedApps = selectedJobFilter 
    ? applications.filter(app => app.jobId === selectedJobFilter)
    : applications;

  return (
    <div className="p-6 relative flex flex-col h-full">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-800">Careers Management</h1>
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem]">{error}</p>}

      {/* TWO COLUMN LAYOUT */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 flex-1 items-start">
        
        {/* --- Left Column: JOBS --- */}
        <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col max-h-[80vh]">
          <div className="px-4 py-3 bg-gray-50 border-b border-gray-100 flex justify-between items-center">
            <h2 className="text-[0.75rem] uppercase text-gray-500 font-semibold tracking-wider">Active Job Postings</h2>
            <button 
              onClick={handleAddJobClick}
              className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2"
            >
              + Create Job
            </button>
          </div>
          
          <div className="overflow-y-auto p-4 space-y-4">
            {jobs.length === 0 ? (
              <p className="text-center text-[0.85rem] text-gray-500 py-4">No jobs created yet.</p>
            ) : (
              jobs.map((job) => {
                const isSelected = selectedJobFilter === (job.id || job._id);
                const activeColor = job.active ? 'border-l-green-500' : 'border-l-gray-300';
                
                return (
                <div 
                  key={job.id || job._id} 
                  onClick={() => setSelectedJobFilter(isSelected ? null : (job.id || job._id))}
                  className={`p-4 border rounded shadow-sm transition-all cursor-pointer hover:shadow-md 
                    ${isSelected ? 'ring-2 ring-blue-500 bg-blue-50/30' : 'bg-white hover:border-blue-300'} 
                    border-l-4 ${activeColor}`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <h3 className="text-[0.9rem] font-bold text-gray-800">{job.title}</h3>
                      <p className="text-[0.8rem] text-gray-500 mt-1 flex items-center gap-2">
                        <span className="font-medium">{job.department}</span> 
                        <span className="text-gray-300">•</span> 
                        <span>{job.location}</span> 
                        <span className="text-gray-300">•</span> 
                        <span>{job.type}</span>
                      </p>
                    </div>
                    <span className={`px-2 py-1 text-[0.7rem] uppercase tracking-wider font-bold rounded-sm ${job.active ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600'}`}>
                      {job.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <div className="mt-4 flex justify-between items-center">
                    <div className="text-[0.75rem] text-blue-600 font-medium">
                      {isSelected ? 'Viewing Applications' : 'Click to view applications'}
                    </div>
                    <div className="flex space-x-2">
                      <button 
                        onClick={(e) => { e.stopPropagation(); handleEditJobClick(job); }} 
                        className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.8rem] px-3 py-1 font-semibold shadow-sm transition-colors"
                      >
                        Edit
                      </button>
                      <button 
                        onClick={(e) => { e.stopPropagation(); handleDeleteJob(job.id || job._id); }} 
                        className="bg-white border border-red-200 text-red-600 hover:bg-red-50 rounded text-[0.8rem] px-3 py-1 font-semibold shadow-sm transition-colors"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              )})
            )}
          </div>
        </div>

        {/* --- Right Column: APPLICATIONS --- */}
        <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden flex flex-col flex-1 max-h-[80vh]">
          <div className="px-4 py-3 bg-gray-50 border-b border-gray-100 flex justify-between items-center">
            <h2 className="text-[0.75rem] uppercase text-gray-500 font-bold tracking-wider">
              {selectedJobFilter ? `Applications for: ${getJobTitle(selectedJobFilter)}` : 'Recent Applications'}
            </h2>
            <div className="flex items-center gap-3">
              {selectedJobFilter && (
                <button 
                  onClick={() => setSelectedJobFilter(null)} 
                  className="text-[0.75rem] text-blue-600 hover:text-blue-800 font-semibold underline"
                >
                  Clear Filter
                </button>
              )}
              <span className="px-2 py-1 bg-white text-gray-600 text-[0.75rem] font-bold rounded-sm border border-gray-200 shadow-sm">
                {displayedApps.length} Total
              </span>
            </div>
          </div>
          
          <div className="overflow-y-auto p-0">
            {displayedApps.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-gray-400">
                <svg className="w-12 h-12 mb-4 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                </svg>
                <p className="text-[0.9rem] font-medium text-gray-500">No applications found.</p>
              </div>
            ) : (
              <table className="w-full text-left border-collapse">
                <thead className="bg-gray-50 border-b border-gray-200 hidden md:table-header-group">
                  <tr>
                    <th className="text-[0.75rem] uppercase text-gray-500 font-bold px-5 py-3 text-left tracking-wider">Applicant Info</th>
                    <th className="text-[0.75rem] uppercase text-gray-500 font-bold px-5 py-3 text-right tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {displayedApps.map((app) => (
                    <tr key={app.id || app._id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors group">
                      <td className="px-5 py-4 text-[0.85rem] text-gray-700">
                        <div className="font-bold text-gray-800 text-[0.9rem]">{app.name}</div>
                        <div className="text-gray-500 mt-0.5">{app.email} • {app.phone}</div>
                        {!selectedJobFilter && (
                          <div className="mt-2 flex items-center gap-1">
                            <span className="text-gray-400 text-[0.75rem]">Applied for:</span> 
                            <span className="bg-blue-50 px-2 py-0.5 rounded text-blue-700 font-medium text-[0.75rem] border border-blue-100">{getJobTitle(app.jobId)}</span>
                          </div>
                        )}
                      </td>
                      <td className="px-5 py-4 text-right space-y-2 md:space-y-0 md:space-x-2 md:whitespace-nowrap">
                        {app.cvUrl && (
                          <a href={app.cvUrl} target="_blank" rel="noopener noreferrer" className="inline-block bg-white border border-blue-200 text-blue-600 hover:bg-blue-50 rounded text-[0.8rem] px-3 py-1 font-semibold shadow-sm transition-colors text-center w-full md:w-auto">
                            View CV
                          </a>
                        )}
                        {app.coverLetter && (
                          <button onClick={() => setSelectedApp(app)} className="inline-block bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.8rem] px-3 py-1 font-semibold shadow-sm transition-colors text-center w-full md:w-auto">
                            Cover Letter
                          </button>
                        )}
                        <button 
                          onClick={() => handleDeleteApplication(app.id || app._id)} 
                          className="inline-block bg-white border border-gray-200 text-red-500 hover:bg-red-50 hover:border-red-200 rounded text-[0.8rem] px-3 py-1 font-semibold opacity-0 group-hover:opacity-100 transition-all text-center w-full md:w-auto"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {/* --- Add / Edit Job Modal --- */}
      {isJobModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded shadow-xl overflow-hidden">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-gray-800">
                {editingJob ? 'Edit Job Posting' : 'Create Job Posting'}
              </h2>
              <button 
                onClick={() => {
                  setIsJobModalOpen(false);
                  setEditingJob(null);
                }} 
                className="text-gray-400 hover:text-red-500 transition-colors"
              >
                <X size={20} />
              </button>
            </div>
            
            <form onSubmit={handleJobSubmit} className="p-5">
              <div className="space-y-4">
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Job Title *</label>
                  <input type="text" name="title" required value={jobForm.title} onChange={handleJobInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" placeholder="e.g. L3 Engineer" />
                </div>
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Department *</label>
                  <input type="text" name="department" required value={jobForm.department} onChange={handleJobInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" placeholder="e.g. Development" />
                </div>
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Location *</label>
                  <input type="text" name="location" required value={jobForm.location} onChange={handleJobInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" placeholder="e.g. Anekal" />
                </div>
                <div>
                  <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Type *</label>
                  <select name="type" value={jobForm.type} onChange={handleJobInputChange} className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white">
                    <option value="Full-time">Full-time</option>
                    <option value="Part-time">Part-time</option>
                    <option value="Contract">Contract</option>
                  </select>
                </div>
                <div className="flex items-center pt-2">
                  <input type="checkbox" id="active" name="active" checked={jobForm.active} onChange={handleJobInputChange} className="w-4 h-4 text-blue-600 border-gray-200 rounded focus:ring-0 focus:ring-offset-0" />
                  <label htmlFor="active" className="ml-2 text-[0.85rem] font-medium text-gray-700 cursor-pointer">Set as Active (Visible on website)</label>
                </div>
              </div>
              <div className="flex justify-end pt-5 space-x-2 mt-5 border-t border-gray-100">
                <button 
                  type="button" 
                  onClick={() => {
                    setIsJobModalOpen(false);
                    setEditingJob(null);
                  }} 
                  className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm transition-colors"
                >
                  Cancel
                </button>
                <button type="submit" disabled={isSubmitting} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-50">
                  {isSubmitting ? 'Saving...' : (editingJob ? 'Update Job' : 'Create Job')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- View Cover Letter Modal --- */}
      {selectedApp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-lg bg-white rounded shadow-xl overflow-hidden">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-xl font-semibold text-gray-800">Cover Letter</h2>
              <button onClick={() => setSelectedApp(null)} className="text-gray-400 hover:text-red-500 transition-colors">
                <X size={20} />
              </button>
            </div>
            <div className="p-5">
              <div className="mb-4 text-[0.85rem] text-gray-600">
                <p><strong>Applicant:</strong> {selectedApp.name}</p>
                <p><strong>Role:</strong> {getJobTitle(selectedApp.jobId)}</p>
              </div>
              <div className="p-4 bg-gray-50 border border-gray-200 rounded whitespace-pre-wrap text-gray-700 text-[0.85rem] leading-relaxed max-h-64 overflow-y-auto">
                {selectedApp.coverLetter}
              </div>
            </div>
            <div className="p-5 border-t border-gray-100 text-right">
              <button onClick={() => setSelectedApp(null)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 font-medium shadow-sm transition-colors">Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}