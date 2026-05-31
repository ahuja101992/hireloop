import React, { useState, useEffect } from 'react';
import { api } from '../utils/api.js';

export default function ReadinessDashboard() {
  const [readiness, setReadiness] = useState(null);
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadReadiness();
  }, []);

  async function loadReadiness() {
    try {
      setLoading(true);
      const data = await api.getReadiness();
      setReadiness(data.overall);
      setCompanies(data.byCompany || []);
    } catch (error) {
      console.error('Failed to load readiness:', error);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return <div className="text-center py-8">Loading readiness data...</div>;
  }

  if (!readiness) {
    return <div className="text-center py-8">No readiness data available</div>;
  }

  return (
    <div className="space-y-6">
      {/* Overall Score */}
      <div className="card p-6">
        <h2 className="text-2xl font-bold mb-6">Overall Readiness</h2>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-blue-50 rounded-lg p-4">
            <div className="text-3xl font-bold text-blue-600">{readiness.overallScore}%</div>
            <div className="text-sm text-gray-600 mt-2">Overall Score</div>
          </div>

          <div className="bg-purple-50 rounded-lg p-4">
            <div className="text-3xl font-bold text-purple-600">{readiness.dsa || 0}%</div>
            <div className="text-sm text-gray-600 mt-2">DSA</div>
          </div>

          <div className="bg-green-50 rounded-lg p-4">
            <div className="text-3xl font-bold text-green-600">{readiness.sd || 0}%</div>
            <div className="text-sm text-gray-600 mt-2">System Design</div>
          </div>

          <div className="bg-orange-50 rounded-lg p-4">
            <div className="text-3xl font-bold text-orange-600">{readiness.behavioral || 0}%</div>
            <div className="text-sm text-gray-600 mt-2">Behavioral</div>
          </div>
        </div>

        {/* Next Topics */}
        {readiness.nextTopics && readiness.nextTopics.length > 0 && (
          <div>
            <h3 className="text-lg font-semibold mb-3">Next Topics to Study (by ROI)</h3>
            <ul className="space-y-2">
              {readiness.nextTopics.slice(0, 5).map((topic, i) => (
                <li key={i} className="flex items-center text-sm">
                  <span className="bg-blue-600 text-white rounded-full w-6 h-6 flex items-center justify-center mr-3 text-xs">
                    {i + 1}
                  </span>
                  <span>{topic}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Company Readiness */}
      <div className="card p-6">
        <h2 className="text-2xl font-bold mb-6">Company Readiness</h2>

        <table>
          <thead>
            <tr>
              <th>Company</th>
              <th>Readiness Score</th>
              <th>Threshold</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {companies.map((company, i) => (
              <tr key={i}>
                <td className="font-medium">{company.name}</td>
                <td>{company.score}%</td>
                <td>{company.threshold}%</td>
                <td>
                  <span className={company.score >= company.threshold ? 'readiness-ready' : 'readiness-not-ready'}>
                    {company.score >= company.threshold ? 'READY' : 'NOT READY'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
