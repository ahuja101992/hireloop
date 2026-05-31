import React, { useState, useEffect } from 'react';
import { api } from '../utils/api.js';

export default function TopicTracker() {
  const [topics, setTopics] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState(null);
  const [editingNotes, setEditingNotes] = useState({});

  useEffect(() => {
    loadTopics();
  }, []);

  async function loadTopics() {
    try {
      setLoading(true);
      const data = await api.getTopics();
      setTopics(data);
    } catch (error) {
      console.error('Failed to load topics:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusChange(topic, newStatus) {
    try {
      await api.updateTopic(topic.id, { status: newStatus });
      setTopics(topics.map(t => t.id === topic.id ? { ...t, status: newStatus } : t));
    } catch (error) {
      console.error('Failed to update topic:', error);
    }
  }

  async function handleNotesChange(topic, newNotes) {
    try {
      await api.updateTopic(topic.id, { notes: newNotes });
      setTopics(topics.map(t => t.id === topic.id ? { ...t, notes: newNotes } : t));
      setEditingId(null);
    } catch (error) {
      console.error('Failed to update notes:', error);
    }
  }

  if (loading) {
    return <div className="text-center py-8">Loading topics...</div>;
  }

  return (
    <div className="card p-6">
      <h2 className="text-2xl font-bold mb-6">Interview Prep Topics</h2>

      <table>
        <thead>
          <tr>
            <th>Category</th>
            <th>Topic</th>
            <th>Frequency</th>
            <th>Status</th>
            <th>Notes</th>
          </tr>
        </thead>
        <tbody>
          {topics.map((topic) => (
            <tr key={topic.id}>
              <td className="font-medium">{topic.category}</td>
              <td>{topic.topic}</td>
              <td>{topic.frequency}</td>
              <td>
                <select
                  value={topic.status || 'NOT_STARTED'}
                  onChange={(e) => handleStatusChange(topic, e.target.value)}
                  className="border border-gray-300 rounded px-2 py-1"
                >
                  <option value="NOT_STARTED">Not Started</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="COVERED">Covered</option>
                  <option value="WEAK">Weak</option>
                </select>
              </td>
              <td>
                {editingId === topic.id ? (
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={editingNotes[topic.id] || topic.notes || ''}
                      onChange={(e) => setEditingNotes({
                        ...editingNotes,
                        [topic.id]: e.target.value
                      })}
                      className="flex-1 border border-gray-300 rounded px-2 py-1"
                      placeholder="Add notes..."
                    />
                    <button
                      onClick={() => handleNotesChange(topic, editingNotes[topic.id] || '')}
                      className="button-primary"
                    >
                      Save
                    </button>
                  </div>
                ) : (
                  <div
                    onClick={() => {
                      setEditingId(topic.id);
                      setEditingNotes({ [topic.id]: topic.notes || '' });
                    }}
                    className="text-gray-600 text-sm cursor-pointer hover:text-gray-900"
                  >
                    {topic.notes ? topic.notes : '(Click to edit)'}
                  </div>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
