import React, { useState, useEffect } from 'react';
import { api } from '../utils/api.js';

export default function PipelineView() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadApplications();
  }, []);

  async function loadApplications() {
    try {
      setLoading(true);
      const data = await api.getApplications();
      setApplications(data);
    } catch (error) {
      console.error('Failed to load applications:', error);
    } finally {
      setLoading(false);
    }
  }

  function groupByStatus(apps) {
    return {
      APPLIED: apps.filter(a => a.pipelineStatus === 'APPLIED'),
      RECRUITER_SCREEN: apps.filter(a => a.pipelineStatus === 'RECRUITER_SCREEN'),
      INTERVIEW: apps.filter(a => a.pipelineStatus === 'INTERVIEW'),
      OFFER: apps.filter(a => a.pipelineStatus === 'OFFER'),
      REJECTED: apps.filter(a => a.pipelineStatus === 'REJECTED'),
    };
  }

  if (loading) {
    return <div className="text-center py-8">Loading applications...</div>;
  }

  const grouped = groupByStatus(applications);
  const statuses = ['APPLIED', 'RECRUITER_SCREEN', 'INTERVIEW', 'OFFER', 'REJECTED'];
  const statusColors = {
    APPLIED: 'bg-blue-100 border-blue-300',
    RECRUITER_SCREEN: 'bg-yellow-100 border-yellow-300',
    INTERVIEW: 'bg-purple-100 border-purple-300',
    OFFER: 'bg-green-100 border-green-300',
    REJECTED: 'bg-red-100 border-red-300',
  };

  return (
    <div className="card p-6">
      <h2 className="text-2xl font-bold mb-6">Application Pipeline</h2>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-4">
        {statuses.map((status) => (
          <div key={status} className="space-y-3">
            <h3 className="font-bold text-sm uppercase text-gray-700">
              {status} ({grouped[status]?.length || 0})
            </h3>

            <div className="space-y-2">
              {grouped[status]?.map((app) => (
                <div
                  key={app.id}
                  className={`p-3 rounded border-l-4 ${statusColors[status]} cursor-pointer hover:shadow-md transition`}
                >
                  <div className="font-semibold text-sm">{app.job?.companyName}</div>
                  <div className="text-xs text-gray-700 mt-1">{app.job?.title}</div>
                  {app.appliedAt && (
                    <div className="text-xs text-gray-500 mt-2">
                      {new Date(app.appliedAt).toLocaleDateString()}
                    </div>
                  )}
                  {app.lastEmailSubject && (
                    <div className="text-xs text-gray-600 mt-2 italic">
                      "{app.lastEmailSubject.substring(0, 30)}..."
                    </div>
                  )}
                </div>
              ))}
            </div>

            {grouped[status]?.length === 0 && (
              <div className="text-center py-4 text-gray-400 text-sm">
                No applications
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="mt-6 text-sm text-gray-600">
        <p>Total applications: {applications.length}</p>
        <p>Offers: {grouped.OFFER?.length || 0}</p>
        <p>Interviews: {grouped.INTERVIEW?.length || 0}</p>
        <p>Rejections: {grouped.REJECTED?.length || 0}</p>
      </div>
    </div>
  );
}
