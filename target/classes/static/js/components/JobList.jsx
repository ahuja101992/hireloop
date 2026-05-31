import React, { useState, useEffect } from 'react';
import { api } from '../utils/api.js';

export default function JobList() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadJobs();
  }, []);

  async function loadJobs() {
    try {
      setLoading(true);
      const data = await api.getJobs();
      setJobs(data);
    } catch (error) {
      console.error('Failed to load jobs:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleConfirm(jobId) {
    try {
      await api.confirmJob(jobId);
      setJobs(jobs.map(j => j.id === jobId ? { ...j, status: 'CONFIRMED' } : j));
    } catch (error) {
      console.error('Failed to confirm job:', error);
    }
  }

  async function handleSkip(jobId) {
    try {
      await api.skipJob(jobId);
      setJobs(jobs.map(j => j.id === jobId ? { ...j, status: 'SKIPPED' } : j));
    } catch (error) {
      console.error('Failed to skip job:', error);
    }
  }

  function getStatusClass(status) {
    switch (status) {
      case 'NEW': return 'status-new';
      case 'SCORED': return 'status-scored';
      case 'CONFIRMED': return 'status-confirmed';
      default: return 'status-new';
    }
  }

  if (loading) {
    return <div className="text-center py-8">Loading jobs...</div>;
  }

  return (
    <div className="card p-6">
      <h2 className="text-2xl font-bold mb-6">Jobs</h2>

      <table>
        <thead>
          <tr>
            <th>Company</th>
            <th>Title</th>
            <th>Score</th>
            <th>Status</th>
            <th>Posted</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((job) => (
            <tr key={job.id}>
              <td className="font-medium">{job.companyName}</td>
              <td>{job.title}</td>
              <td className="font-bold text-lg">{job.fitScore || '-'}</td>
              <td>
                <span className={`status-badge ${getStatusClass(job.status)}`}>
                  {job.status}
                </span>
              </td>
              <td className="text-sm text-gray-600">
                {job.postedAt ? new Date(job.postedAt).toLocaleDateString() : '-'}
              </td>
              <td className="space-x-2">
                {job.tailoredResumeJson && (
                  <a
                    href={`/jobs/${job.id}/diff`}
                    className="button-secondary text-sm"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    View Diff
                  </a>
                )}
                {job.status !== 'CONFIRMED' && job.status !== 'SKIPPED' && (
                  <>
                    <button
                      onClick={() => handleConfirm(job.id)}
                      className="button-primary text-sm"
                    >
                      Confirm
                    </button>
                    <button
                      onClick={() => handleSkip(job.id)}
                      className="button-secondary text-sm"
                    >
                      Skip
                    </button>
                  </>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {jobs.length === 0 && (
        <div className="text-center py-8 text-gray-500">
          No jobs found. Check back soon!
        </div>
      )}
    </div>
  );
}
