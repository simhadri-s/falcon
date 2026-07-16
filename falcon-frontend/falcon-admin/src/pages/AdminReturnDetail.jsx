import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Package, User, ClipboardList, CheckCircle, XCircle, Clock, Truck, RefreshCcw } from 'lucide-react';
import api from '../services/api';

export default function AdminReturnDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [returnRequest, setReturnRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updating, setUpdating] = useState(false);
  const [adminComment, setAdminComment] = useState('');

  const fetchReturnDetail = async () => {
    setLoading(true);
    try {
      const response = await api.get(`/api/returns/${id}`);
      setReturnRequest(response.data);
      setAdminComment(response.data.adminComment || '');
    } catch (err) {
      setError("Failed to fetch return details.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReturnDetail();
  }, [id]);

  const updateStatus = async (status) => {
    setUpdating(true);
    try {
      await api.patch(`/api/returns/${id}/status?status=${status}&adminComment=${adminComment}`);
      fetchReturnDetail();
    } catch (err) {
      alert("Failed to update status.");
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <div className="p-8 text-center">Loading return details...</div>;
  if (error) return <div className="p-8 text-center text-red-500">{error}</div>;
  if (!returnRequest) return <div className="p-8 text-center">Return request not found.</div>;

  const getStatusColor = (status) => {
    switch (status?.toUpperCase()) {
      case 'RETURN_REQUESTED': return 'text-orange-600 bg-orange-50 border-orange-200';
      case 'RETURN_APPROVED': return 'text-blue-600 bg-blue-50 border-blue-200';
      case 'RETURN_REJECTED': return 'text-red-600 bg-red-50 border-red-200';
      case 'RETURN_PICKED_UP': return 'text-purple-600 bg-purple-50 border-purple-200';
      case 'RETURN_COMPLETED': return 'text-green-600 bg-green-50 border-green-200';
      default: return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <button onClick={() => navigate(-1)} className="flex items-center text-[0.85rem] text-gray-500 hover:text-gray-800 mb-6 font-medium transition-colors">
        <ArrowLeft className="w-4 h-4 mr-1.5" /> Back to Returns
      </button>

      <div className="flex flex-col md:flex-row justify-between items-start gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
            Return Request #{returnRequest.id.slice(-8).toUpperCase()}
          </h1>
          <p className="text-[0.85rem] text-gray-500 font-medium mt-1">Order ID: #{returnRequest.orderId.slice(-8).toUpperCase()}</p>
        </div>
        <div className={`px-3 py-1 rounded border text-[0.70rem] font-medium uppercase tracking-wider ${getStatusColor(returnRequest.status)}`}>
          {returnRequest.status.replace('_', ' ')}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-white p-5 rounded border border-gray-100 shadow-sm">
          <div className="flex items-center gap-2 mb-3 text-gray-500">
            <User className="w-4 h-4" />
            <h3 className="font-medium uppercase text-[0.70rem] tracking-wider">Customer</h3>
          </div>
          <p className="font-medium text-[0.85rem] text-gray-800 break-all">{returnRequest.userId}</p>
        </div>
        <div className="bg-white p-5 rounded border border-gray-100 shadow-sm">
          <div className="flex items-center gap-2 mb-3 text-gray-500">
            <Clock className="w-4 h-4" />
            <h3 className="font-medium uppercase text-[0.70rem] tracking-wider">Requested On</h3>
          </div>
          <p className="font-medium text-[0.85rem] text-gray-800">{new Date(returnRequest.createdAt).toLocaleString()}</p>
        </div>
        <div className="bg-white p-5 rounded border border-gray-100 shadow-sm">
          <div className="flex items-center gap-2 mb-3 text-gray-500">
            <ClipboardList className="w-4 h-4" />
            <h3 className="font-medium uppercase text-[0.70rem] tracking-wider">Reason</h3>
          </div>
          <p className="font-medium text-[0.85rem] text-gray-800">{returnRequest.reason}</p>
        </div>
      </div>

      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden mb-6">
        <div className="px-5 py-4 border-b border-gray-100 bg-gray-50">
          <h3 className="text-[0.90rem] font-semibold text-gray-800 flex items-center gap-2">
            <Package className="w-4 h-4 text-gray-500" /> Items to Return
          </h3>
        </div>
        <div className="p-5">
          <div className="space-y-3">
            {returnRequest.items.map((item, index) => (
              <div key={index} className="flex items-center justify-between p-3 rounded border border-gray-100 bg-gray-50/50">
                <div className="flex items-center gap-4">
                  {item.productSnapshot?.imageUrls?.[0] ? (
                    <img src={item.productSnapshot.imageUrls[0]} className="w-10 h-10 object-cover rounded border border-gray-200 bg-white" alt="" />
                  ) : (
                    <div className="w-10 h-10 bg-gray-100 rounded border border-gray-200 flex items-center justify-center text-[10px] text-gray-400">No Img</div>
                  )}
                  <div>
                    <p className="font-medium text-[0.85rem] text-gray-800">{item.productSnapshot?.name || 'Unknown Product'}</p>
                    <p className="text-[0.75rem] text-gray-500 font-mono mt-0.5">Code: {item.productSnapshot?.productCode || 'N/A'}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-[0.70rem] font-medium text-gray-400 uppercase tracking-wider mb-0.5">Return Qty</p>
                  <p className="text-[0.95rem] font-semibold text-gray-800">{item.quantity}</p>
                </div>
              </div>
            ))}
          </div>

          {returnRequest.comment && (
            <div className="mt-4 p-4 rounded bg-gray-50 border border-gray-100">
              <p className="text-[0.75rem] font-semibold text-gray-500 uppercase tracking-wider mb-1">Customer Comment</p>
              <p className="text-[0.85rem] text-gray-700 italic">"{returnRequest.comment}"</p>
            </div>
          )}
        </div>
      </div>

      <div className="bg-white rounded border border-gray-100 shadow-sm overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100 bg-gray-50">
          <h3 className="text-[0.90rem] font-semibold text-gray-800">Process Return</h3>
        </div>
        <div className="p-5">
          <div className="mb-5">
            <label className="block text-[0.80rem] font-medium text-gray-700 mb-1.5">Admin Comment / Note</label>
            <textarea 
              value={adminComment}
              onChange={(e) => setAdminComment(e.target.value)}
              placeholder="Internal notes or reason for rejection..."
              className="w-full px-3 py-2 text-[0.85rem] border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none transition-colors"
              rows="3"
            />
          </div>

          <div className="flex flex-wrap gap-3">
            {returnRequest.status === 'RETURN_REQUESTED' && (
              <>
                <button 
                  onClick={() => updateStatus('RETURN_APPROVED')}
                  disabled={updating}
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-[0.85rem] font-medium rounded shadow-sm flex items-center gap-1.5 transition-colors disabled:opacity-50"
                >
                  <CheckCircle className="w-4 h-4" /> Approve & Initiate Refund
                </button>
                <button 
                  onClick={() => updateStatus('RETURN_REJECTED')}
                  disabled={updating}
                  className="px-4 py-2 bg-white border border-gray-200 text-red-600 hover:bg-red-50 text-[0.85rem] font-medium rounded shadow-sm flex items-center gap-1.5 transition-colors disabled:opacity-50"
                >
                  <XCircle className="w-4 h-4" /> Reject Return
                </button>
              </>
            )}

            {returnRequest.status === 'RETURN_APPROVED' && (
              <button 
                onClick={() => updateStatus('RETURN_PICKED_UP')}
                disabled={updating}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-[0.85rem] font-medium rounded shadow-sm flex items-center gap-1.5 transition-colors disabled:opacity-50"
              >
                <Truck className="w-4 h-4" /> Mark as Picked Up
              </button>
            )}

            {returnRequest.status === 'RETURN_PICKED_UP' && (
              <button 
                onClick={() => updateStatus('RETURN_COMPLETED')}
                disabled={updating}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-[0.85rem] font-medium rounded shadow-sm flex items-center gap-1.5 transition-colors disabled:opacity-50"
              >
                <RefreshCcw className="w-4 h-4" /> Mark as Completed
              </button>
            )}

            {returnRequest.status === 'RETURN_REJECTED' && (
              <button 
                onClick={() => updateStatus('RETURN_REQUESTED')}
                disabled={updating}
                className="px-4 py-2 bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 text-[0.85rem] font-medium rounded shadow-sm flex items-center gap-1.5 transition-colors disabled:opacity-50"
              >
                Re-open Request
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
