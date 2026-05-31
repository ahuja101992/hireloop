import React, { useState, useEffect } from 'react';
import { api } from '../utils/api.js';

export default function SettingsModal({ onClose }) {
  const [activeTab, setActiveTab] = useState('filters');
  const [filters, setFilters] = useState({});
  const [companies, setCompanies] = useState([]);
  const [llmProvider, setLlmProvider] = useState('claude');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadSettings();
  }, []);

  async function loadSettings() {
    try {
      const filtersData = await api.getConfig('filters');
      const companiesData = await api.getConfig('companies');
      const configData = await api.getConfig('general');

      setFilters(filtersData);
      setCompanies(companiesData || []);
      setLlmProvider(configData.llmProvider || 'claude');
    } catch (error) {
      console.error('Failed to load settings:', error);
    }
  }

  async function handleSaveFilters() {
    try {
      setSaving(true);
      await api.updateConfig('filters', filters);
      alert('Filters saved!');
    } catch (error) {
      console.error('Failed to save filters:', error);
      alert('Failed to save filters');
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveCompanies() {
    try {
      setSaving(true);
      await api.updateConfig('companies', companies);
      alert('Companies saved!');
    } catch (error) {
      console.error('Failed to save companies:', error);
      alert('Failed to save companies');
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveLLM() {
    try {
      setSaving(true);
      await api.updateConfig('general', { llmProvider });
      alert('LLM provider saved!');
    } catch (error) {
      console.error('Failed to save LLM provider:', error);
      alert('Failed to save LLM provider');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop">
      <div className="modal">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold">Settings</h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 text-2xl"
          >
            ×
          </button>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-gray-200 mb-6">
          <button
            onClick={() => setActiveTab('filters')}
            className={`px-4 py-2 font-medium ${
              activeTab === 'filters'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-600'
            }`}
          >
            Filters
          </button>
          <button
            onClick={() => setActiveTab('companies')}
            className={`px-4 py-2 font-medium ${
              activeTab === 'companies'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-600'
            }`}
          >
            Companies
          </button>
          <button
            onClick={() => setActiveTab('llm')}
            className={`px-4 py-2 font-medium ${
              activeTab === 'llm'
                ? 'border-b-2 border-blue-500 text-blue-600'
                : 'text-gray-600'
            }`}
          >
            LLM
          </button>
        </div>

        {/* Filters Tab */}
        {activeTab === 'filters' && (
          <div className="space-y-4 mb-6">
            <div className="form-group">
              <label>Max Age (days)</label>
              <input
                type="number"
                value={filters.maxAge || 30}
                onChange={(e) => setFilters({ ...filters, maxAge: parseInt(e.target.value) })}
              />
            </div>

            <div className="form-group">
              <label>Min Fit Score</label>
              <input
                type="number"
                min="0"
                max="100"
                value={filters.minFitScore || 70}
                onChange={(e) => setFilters({ ...filters, minFitScore: parseInt(e.target.value) })}
              />
            </div>

            <div className="form-group">
              <label>Keywords (comma-separated)</label>
              <input
                type="text"
                value={filters.keywords || ''}
                onChange={(e) => setFilters({ ...filters, keywords: e.target.value })}
                placeholder="e.g., Python, Cloud, Architecture"
              />
            </div>

            <div className="form-group">
              <label>Levels</label>
              <input
                type="text"
                value={filters.levels || ''}
                onChange={(e) => setFilters({ ...filters, levels: e.target.value })}
                placeholder="e.g., Principal, Senior"
              />
            </div>

            <div className="form-group">
              <label>Locations</label>
              <input
                type="text"
                value={filters.locations || ''}
                onChange={(e) => setFilters({ ...filters, locations: e.target.value })}
                placeholder="e.g., Austin, San Francisco"
              />
            </div>

            <button onClick={handleSaveFilters} className="button-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Filters'}
            </button>
          </div>
        )}

        {/* Companies Tab */}
        {activeTab === 'companies' && (
          <div className="space-y-4 mb-6">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>ATS</th>
                  <th>Priority</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {companies.map((company, i) => (
                  <tr key={i}>
                    <td>{company.name}</td>
                    <td>{company.ats}</td>
                    <td>{company.priority}</td>
                    <td>
                      <button
                        onClick={() => setCompanies(companies.filter((_, idx) => idx !== i))}
                        className="button-danger text-xs"
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <button onClick={handleSaveCompanies} className="button-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Companies'}
            </button>
          </div>
        )}

        {/* LLM Tab */}
        {activeTab === 'llm' && (
          <div className="space-y-4 mb-6">
            <div className="form-group">
              <label>LLM Provider</label>
              <select
                value={llmProvider}
                onChange={(e) => setLlmProvider(e.target.value)}
              >
                <option value="claude">Claude</option>
                <option value="gemini">Gemini</option>
              </select>
            </div>

            <button onClick={handleSaveLLM} className="button-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save LLM Provider'}
            </button>
          </div>
        )}

        {/* Close Button */}
        <div className="flex justify-end">
          <button onClick={onClose} className="button-secondary">
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
