import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function InquiryDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [inquiry, setInquiry] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchSingleInquiry = async () => {
      try {
        // 1. Fetch the inquiry details
        const response = await api.get(`/api/inquiries/${id}`);
        const fetchedInquiry = response.data;
        setInquiry(fetchedInquiry);

        // 2. Automatically update to READ if it is currently NEW
        if (fetchedInquiry.status === 'NEW') {
          await api.put(`/api/inquiries/${id}/status`, { status: 'READ' });
          
          // Update the local UI state so the badge changes instantly
          setInquiry(prev => ({ ...prev, status: 'READ' }));
        }

      } catch (err) {
        console.error("Error fetching/updating inquiry:", err);
        setError('Failed to load inquiry details.');
      } finally {
        setLoading(false);
      }
    };

    fetchSingleInquiry();
  }, [id]);

  const handleUpdateStatus = async (newStatus) => {
    try {
      await api.put(`/api/inquiries/${id}/status`, { status: newStatus });
      
      setInquiry({ ...inquiry, status: newStatus });
    } catch (err) {
      console.error("Failed to update status", err);
      alert("Failed to update status. Please try again.");
    }
  };

  const formatDate = (dateString) => {
    const options = { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString('en-US', options);
  };

  if (loading) return <div className="p-8 text-center text-gray-500">Loading inquiry details...</div>;
  if (error) return <div className="p-8 text-center text-red-500">{error}</div>;
  if (!inquiry) return <div className="p-8 text-center text-gray-500">Inquiry not found.</div>;

  return (
    <div className="max-w-3xl mx-auto">
      {/* Back Button */}
      <button 
        onClick={() => navigate('/admin/inquiries')}
        className="mb-6 text-[#1E3A5F] hover:underline flex items-center gap-2 font-medium"
      >
        &larr; Back to Inquiries
      </button>

      <div className="p-8 bg-white rounded shadow-md">
        <div className="flex items-start justify-between mb-6 pb-6 border-b">
          <div>
            <h1 className="text-2xl font-bold text-[#1E3A5F] mb-1">{inquiry.subject}</h1>
            <p className="text-sm text-gray-500">Received: {formatDate(inquiry.createdAt)}</p>
          </div>
          <span className={`px-3 py-1 text-sm font-bold rounded-full ${
            inquiry.status === 'NEW' ? 'text-red-800 bg-red-100' :
            inquiry.status === 'READ' ? 'text-blue-800 bg-blue-100' :
            'text-green-800 bg-green-100'
          }`}>
            {inquiry.status}
          </span>
        </div>
        
        <div className="grid grid-cols-2 gap-4 mb-8 text-gray-700">
          <div>
            <p className="text-sm text-gray-500">Sender Name</p>
            <p className="font-medium">{inquiry.name}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Email Address</p>
            <p className="font-medium"><a href={`mailto:${inquiry.email}`} className="text-blue-600 hover:underline">{inquiry.email}</a></p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Phone Number</p>
            <p className="font-medium">{inquiry.phone || 'N/A'}</p>
          </div>
        </div>

        <div className="p-6 bg-gray-50 rounded border border-gray-100">
          <p className="text-sm text-gray-500 mb-2">Message</p>
          <p className="whitespace-pre-wrap text-gray-800 leading-relaxed">{inquiry.message}</p>
        </div>

        {/* Action Buttons */}
        <div className="flex gap-4 pt-8 mt-8 border-t">
          {inquiry.status !== 'RESPONDED' && (
            <button 
              onClick={() => handleUpdateStatus('RESPONDED')}
              className="px-6 py-2 text-white bg-green-600 rounded hover:bg-green-700 font-medium"
            >
              Mark as Responded
            </button>
          )}
        </div>
      </div>
    </div>
  );
}