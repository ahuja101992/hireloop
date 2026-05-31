const { useState, useEffect } = React;

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [showSettings, setShowSettings] = useState(false);
  const [autoApplySettings, setAutoApplySettings] = useState({ enabled: false, headless: true });
  const [emailPreferences, setEmailPreferences] = useState({
    notifyNewJobs: true,
    notifyResumeChanges: true,
    notifyApplications: true,
    digestFrequency: 'immediate'
  });
  const [targetCompanies, setTargetCompanies] = useState([]);
  const [newCompanyInput, setNewCompanyInput] = useState('');
  const [resumeStatus, setResumeStatus] = useState({ exists: false, size: 0 });
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsSaved, setSettingsSaved] = useState(false);
  const [resumeUploading, setResumeUploading] = useState(false);
  const [resumeUploaded, setResumeUploaded] = useState(false);

  useEffect(() => {
    if (showSettings) {
      loadSettings();
      loadResumeStatus();
    }
  }, [showSettings]);

  const loadSettings = async () => {
    try {
      setSettingsLoading(true);
      const applyResponse = await fetch('/api/config/apply-engine');
      const applyData = await applyResponse.json();
      setAutoApplySettings(applyData);

      const emailResponse = await fetch('/api/config/email-preferences');
      const emailData = await emailResponse.json();
      setEmailPreferences(emailData);

      const companiesResponse = await fetch('/api/config/target-companies');
      const companiesData = await companiesResponse.json();
      setTargetCompanies(companiesData.companies || []);
    } catch (error) {
      console.error('Failed to load settings:', error);
    } finally {
      setSettingsLoading(false);
    }
  };

  const saveSettings = async () => {
    try {
      setSettingsLoading(true);
      const applyResponse = await fetch('/api/config/apply-engine', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(autoApplySettings)
      });
      const applyData = await applyResponse.json();
      setAutoApplySettings(applyData);

      const emailResponse = await fetch('/api/config/email-preferences', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(emailPreferences)
      });
      const emailData = await emailResponse.json();
      setEmailPreferences(emailData);

      const companiesResponse = await fetch('/api/config/target-companies', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ companies: targetCompanies })
      });
      const companiesData = await companiesResponse.json();
      setTargetCompanies(companiesData.companies || []);

      setSettingsSaved(true);
      setTimeout(() => setSettingsSaved(false), 3000);
    } catch (error) {
      console.error('Failed to save settings:', error);
    } finally {
      setSettingsLoading(false);
    }
  };

  const loadResumeStatus = async () => {
    try {
      const response = await fetch('/api/resume/status');
      const data = await response.json();
      setResumeStatus(data);
    } catch (error) {
      console.error('Failed to load resume status:', error);
    }
  };

  const handleResumeUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      setResumeUploading(true);
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch('/api/resume/upload', {
        method: 'POST',
        body: formData
      });
      const data = await response.json();

      if (data.success) {
        setResumeUploaded(true);
        await loadResumeStatus();
        setTimeout(() => setResumeUploaded(false), 3000);
      }
    } catch (error) {
      console.error('Failed to upload resume:', error);
    } finally {
      setResumeUploading(false);
    }
  };

  const addCompany = () => {
    if (newCompanyInput.trim() && !targetCompanies.includes(newCompanyInput.trim())) {
      setTargetCompanies([...targetCompanies, newCompanyInput.trim()]);
      setNewCompanyInput('');
    }
  };

  const removeCompany = (company) => {
    setTargetCompanies(targetCompanies.filter(c => c !== company));
  };

  const tabClass = (tab) => activeTab === tab
    ? 'py-4 px-2 border-b-2 font-medium text-sm border-blue-500 text-blue-600 cursor-pointer'
    : 'py-4 px-2 border-b-2 font-medium text-sm border-transparent text-gray-500 hover:text-gray-700 cursor-pointer';

  const renderContent = () => {
    switch(activeTab) {
      case 'dashboard':
        return React.createElement('div', { className: 'card p-6' },
          React.createElement('h2', { className: 'text-2xl font-bold mb-6' }, 'Overall Readiness'),
          React.createElement('div', { className: 'grid grid-cols-1 md:grid-cols-4 gap-4 mb-6' },
            React.createElement('div', { className: 'bg-blue-50 rounded-lg p-4' },
              React.createElement('div', { className: 'text-3xl font-bold text-blue-600' }, '0%'),
              React.createElement('div', { className: 'text-sm text-gray-600 mt-2' }, 'Overall Score')
            ),
            React.createElement('div', { className: 'bg-purple-50 rounded-lg p-4' },
              React.createElement('div', { className: 'text-3xl font-bold text-purple-600' }, '0%'),
              React.createElement('div', { className: 'text-sm text-gray-600 mt-2' }, 'DSA')
            ),
            React.createElement('div', { className: 'bg-green-50 rounded-lg p-4' },
              React.createElement('div', { className: 'text-3xl font-bold text-green-600' }, '0%'),
              React.createElement('div', { className: 'text-sm text-gray-600 mt-2' }, 'System Design')
            ),
            React.createElement('div', { className: 'bg-orange-50 rounded-lg p-4' },
              React.createElement('div', { className: 'text-3xl font-bold text-orange-600' }, '0%'),
              React.createElement('div', { className: 'text-sm text-gray-600 mt-2' }, 'Behavioral')
            )
          )
        );
      case 'topics':
        return React.createElement('div', { className: 'card p-6' },
          React.createElement('h2', { className: 'text-2xl font-bold mb-6' }, 'Topics to Study'),
          React.createElement('p', { className: 'text-gray-600' }, 'Topic tracking and progress')
        );
      case 'jobs':
        return React.createElement('div', { className: 'card p-6' },
          React.createElement('h2', { className: 'text-2xl font-bold mb-6' }, 'Jobs'),
          React.createElement('p', { className: 'text-gray-600' }, 'No jobs available yet')
        );
      case 'pipeline':
        return React.createElement('div', { className: 'card p-6' },
          React.createElement('h2', { className: 'text-2xl font-bold mb-6' }, 'Application Pipeline'),
          React.createElement('p', { className: 'text-gray-600' }, 'Track your applications')
        );
      default:
        return null;
    }
  };

  return React.createElement('div', { className: 'min-h-screen bg-gray-50' },
    React.createElement('header', { className: 'bg-white shadow' },
      React.createElement('div', { className: 'max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex justify-between items-center' },
        React.createElement('h1', { className: 'text-3xl font-bold text-blue-600' }, 'HireLoop'),
        React.createElement('button', {
          onClick: () => setShowSettings(true),
          className: 'bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded cursor-pointer'
        }, '⚙️ Settings')
      )
    ),
    React.createElement('nav', { className: 'bg-white border-b border-gray-200' },
      React.createElement('div', { className: 'max-w-7xl mx-auto px-4 sm:px-6 lg:px-8' },
        React.createElement('div', { className: 'flex space-x-8' },
          React.createElement('button', {
            onClick: () => setActiveTab('dashboard'),
            className: tabClass('dashboard')
          }, 'Dashboard'),
          React.createElement('button', {
            onClick: () => setActiveTab('topics'),
            className: tabClass('topics')
          }, 'Topics'),
          React.createElement('button', {
            onClick: () => setActiveTab('jobs'),
            className: tabClass('jobs')
          }, 'Jobs'),
          React.createElement('button', {
            onClick: () => setActiveTab('pipeline'),
            className: tabClass('pipeline')
          }, 'Pipeline')
        )
      )
    ),
    React.createElement('main', { className: 'max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8' },
      renderContent()
    ),
    showSettings && React.createElement('div', { className: 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50' },
      React.createElement('div', { className: 'bg-white rounded-lg p-8 max-w-2xl w-full max-h-screen overflow-y-auto' },
        React.createElement('h2', { className: 'text-2xl font-bold mb-6' }, 'Settings'),

        // Auto-Apply Settings Section
        React.createElement('div', { className: 'mb-8 pb-8 border-b' },
          React.createElement('h3', { className: 'text-lg font-semibold mb-4' }, 'Auto-Apply Engine'),
          React.createElement('div', { className: 'space-y-4' },
            // Enable Toggle
            React.createElement('div', { className: 'flex items-center justify-between' },
              React.createElement('div', null,
                React.createElement('label', { className: 'block font-medium text-gray-700' }, 'Enable Auto-Apply'),
                React.createElement('p', { className: 'text-sm text-gray-500' }, 'Automatically apply to matching jobs')
              ),
              React.createElement('input', {
                type: 'checkbox',
                checked: autoApplySettings.enabled,
                onChange: (e) => setAutoApplySettings({ ...autoApplySettings, enabled: e.target.checked }),
                className: 'w-5 h-5 cursor-pointer',
                disabled: settingsLoading
              })
            ),
            // Headless Mode Toggle
            React.createElement('div', { className: 'flex items-center justify-between' },
              React.createElement('div', null,
                React.createElement('label', { className: 'block font-medium text-gray-700' }, 'Headless Mode'),
                React.createElement('p', { className: 'text-sm text-gray-500' }, 'Run browser in background (no visual window)')
              ),
              React.createElement('input', {
                type: 'checkbox',
                checked: autoApplySettings.headless,
                onChange: (e) => setAutoApplySettings({ ...autoApplySettings, headless: e.target.checked }),
                className: 'w-5 h-5 cursor-pointer',
                disabled: settingsLoading
              })
            )
          )
        ),

        // Resume Upload Section
        React.createElement('div', { className: 'mb-8 pb-8 border-b' },
          React.createElement('h3', { className: 'text-lg font-semibold mb-4' }, 'Resume'),
          React.createElement('div', { className: 'space-y-4' },
            React.createElement('div', null,
              resumeStatus.exists && React.createElement('div', { className: 'mb-4 p-3 bg-blue-100 text-blue-800 rounded' },
                '✓ Resume uploaded: ' + (resumeStatus.size / 1024).toFixed(1) + ' KB'
              ),
              !resumeStatus.exists && React.createElement('div', { className: 'mb-4 p-3 bg-yellow-100 text-yellow-800 rounded' },
                '⚠️ No resume uploaded'
              )
            ),
            React.createElement('div', null,
              React.createElement('input', {
                type: 'file',
                id: 'resume-upload',
                onChange: handleResumeUpload,
                disabled: resumeUploading,
                className: 'hidden',
                accept: '.pdf,.doc,.docx'
              }),
              React.createElement('button', {
                onClick: () => document.getElementById('resume-upload').click(),
                className: 'w-full px-4 py-2 bg-blue-500 hover:bg-blue-700 text-white rounded font-medium cursor-pointer disabled:opacity-50',
                disabled: resumeUploading
              }, resumeUploading ? 'Uploading...' : 'Upload Resume')
            ),
            resumeUploaded && React.createElement('div', { className: 'p-3 bg-green-100 text-green-800 rounded' },
              '✓ Resume uploaded successfully'
            )
          )
        ),

        // Email Preferences Section
        React.createElement('div', { className: 'mb-8 pb-8 border-b' },
          React.createElement('h3', { className: 'text-lg font-semibold mb-4' }, 'Email Notifications'),
          React.createElement('div', { className: 'space-y-4' },
            React.createElement('div', { className: 'flex items-center justify-between' },
              React.createElement('label', { className: 'font-medium text-gray-700' }, 'New Jobs'),
              React.createElement('input', {
                type: 'checkbox',
                checked: emailPreferences.notifyNewJobs,
                onChange: (e) => setEmailPreferences({ ...emailPreferences, notifyNewJobs: e.target.checked }),
                className: 'w-5 h-5 cursor-pointer',
                disabled: settingsLoading
              })
            ),
            React.createElement('div', { className: 'flex items-center justify-between' },
              React.createElement('label', { className: 'font-medium text-gray-700' }, 'Resume Changes'),
              React.createElement('input', {
                type: 'checkbox',
                checked: emailPreferences.notifyResumeChanges,
                onChange: (e) => setEmailPreferences({ ...emailPreferences, notifyResumeChanges: e.target.checked }),
                className: 'w-5 h-5 cursor-pointer',
                disabled: settingsLoading
              })
            ),
            React.createElement('div', { className: 'flex items-center justify-between' },
              React.createElement('label', { className: 'font-medium text-gray-700' }, 'Application Status'),
              React.createElement('input', {
                type: 'checkbox',
                checked: emailPreferences.notifyApplications,
                onChange: (e) => setEmailPreferences({ ...emailPreferences, notifyApplications: e.target.checked }),
                className: 'w-5 h-5 cursor-pointer',
                disabled: settingsLoading
              })
            ),
            React.createElement('div', null,
              React.createElement('label', { className: 'block font-medium text-gray-700 mb-2' }, 'Notification Frequency'),
              React.createElement('select', {
                value: emailPreferences.digestFrequency,
                onChange: (e) => setEmailPreferences({ ...emailPreferences, digestFrequency: e.target.value }),
                className: 'w-full px-3 py-2 border border-gray-300 rounded',
                disabled: settingsLoading
              },
                React.createElement('option', { value: 'immediate' }, 'Immediate'),
                React.createElement('option', { value: 'daily' }, 'Daily Digest'),
                React.createElement('option', { value: 'weekly' }, 'Weekly Digest')
              )
            )
          )
        ),

        // Target Companies Section
        React.createElement('div', { className: 'mb-8 pb-8 border-b' },
          React.createElement('h3', { className: 'text-lg font-semibold mb-4' }, 'Target Companies'),
          React.createElement('div', { className: 'space-y-4' },
            React.createElement('div', { className: 'flex gap-2' },
              React.createElement('input', {
                type: 'text',
                value: newCompanyInput,
                onChange: (e) => setNewCompanyInput(e.target.value),
                onKeyPress: (e) => e.key === 'Enter' && addCompany(),
                placeholder: 'Add a company...',
                className: 'flex-1 px-3 py-2 border border-gray-300 rounded',
                disabled: settingsLoading
              }),
              React.createElement('button', {
                onClick: addCompany,
                className: 'px-4 py-2 bg-blue-500 hover:bg-blue-700 text-white rounded cursor-pointer disabled:opacity-50',
                disabled: settingsLoading || !newCompanyInput.trim()
              }, 'Add')
            ),
            React.createElement('div', { className: 'flex flex-wrap gap-2' },
              targetCompanies.map((company, i) =>
                React.createElement('div', { key: i, className: 'bg-gray-200 px-3 py-1 rounded-full flex items-center gap-2' },
                  React.createElement('span', null, company),
                  React.createElement('button', {
                    onClick: () => removeCompany(company),
                    className: 'font-bold cursor-pointer hover:text-red-600',
                    disabled: settingsLoading
                  }, '✕')
                )
              )
            )
          )
        ),

        // Status Message
        settingsSaved && React.createElement('div', { className: 'mb-4 p-3 bg-green-100 text-green-800 rounded' },
          '✓ Settings saved successfully'
        ),

        // Action Buttons
        React.createElement('div', { className: 'flex justify-end space-x-3' },
          React.createElement('button', {
            onClick: () => setShowSettings(false),
            className: 'px-4 py-2 text-gray-700 bg-gray-200 hover:bg-gray-300 rounded font-medium cursor-pointer',
            disabled: settingsLoading
          }, 'Close'),
          React.createElement('button', {
            onClick: saveSettings,
            className: 'px-4 py-2 bg-blue-500 hover:bg-blue-700 text-white rounded font-medium cursor-pointer disabled:opacity-50',
            disabled: settingsLoading
          }, settingsLoading ? 'Saving...' : 'Save Settings')
        )
      )
    )
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(React.createElement(App));
