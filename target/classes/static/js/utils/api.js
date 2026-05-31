const API_BASE = '/api';

async function apiCall(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;

  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`API error: ${response.status} ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`API call failed: ${endpoint}`, error);
    throw error;
  }
}

export const api = {
  // Topics
  getTopics: () => apiCall('/topics'),
  updateTopic: (id, data) => apiCall(`/topics/${id}/update`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  // Jobs
  getJobs: () => apiCall('/jobs'),
  getJob: (id) => apiCall(`/jobs/${id}`),
  scoreJob: (id) => apiCall(`/jobs/${id}/score`, { method: 'POST' }),
  confirmJob: (id) => apiCall(`/jobs/${id}/confirm`, { method: 'POST' }),
  skipJob: (id) => apiCall(`/jobs/${id}/skip`, { method: 'POST' }),

  // Readiness
  getReadiness: () => apiCall('/readiness'),
  getCompanyReadiness: (company) => apiCall(`/readiness/${company}`),

  // Applications
  getApplications: () => apiCall('/applications'),
  updateApplicationStatus: (id, status) => apiCall(`/applications/${id}/status`, {
    method: 'POST',
    body: JSON.stringify({ status }),
  }),

  // Config
  getConfig: (section) => apiCall(`/config/${section}`),
  updateConfig: (section, data) => apiCall(`/config/${section}`, {
    method: 'POST',
    body: JSON.stringify(data),
  }),

  // Resume
  loadResume: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return fetch(`${API_BASE}/resume/load`, {
      method: 'POST',
      body: formData,
    }).then(r => r.json());
  },
};
