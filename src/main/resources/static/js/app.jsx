const { useState } = React;

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [showSettings, setShowSettings] = useState(false);

  const tabClass = (tab) => activeTab === tab
    ? 'py-4 px-2 border-b-2 font-medium text-sm border-blue-500 text-blue-600'
    : 'py-4 px-2 border-b-2 font-medium text-sm border-transparent text-gray-500 hover:text-gray-700';

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
          className: 'bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded'
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
    showSettings && React.createElement('div', { className: 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center' },
      React.createElement('div', { className: 'bg-white rounded-lg p-8 max-w-md w-full' },
        React.createElement('h2', { className: 'text-2xl font-bold mb-4' }, 'Settings'),
        React.createElement('p', { className: 'text-gray-600 mb-6' }, 'Settings panel coming soon'),
        React.createElement('button', {
          onClick: () => setShowSettings(false),
          className: 'bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded'
        }, 'Close')
      )
    )
  );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(React.createElement(App));
