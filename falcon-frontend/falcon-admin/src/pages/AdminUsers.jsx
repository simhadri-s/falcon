import { useEffect, useState } from 'react';
import { Search, FilterX, ArrowUpDown, ArrowUp, ArrowDown, X } from 'lucide-react';
import api from '../services/api';

const AVAILABLE_PERMISSIONS = [ 
  "ALL_PERMISSIONS", 
  "MANAGE_BANNERS", 
  "MANAGE_CATEGORIES", 
  "MANAGE_COMPANYIMAGES", 
  "MANAGE_COMPANYSETTINGS", 
  "MANAGE_DELIVERYLOCATIONS", 
  "MANAGE_INDUSTRIES", 
  "MANAGE_INQUIRIES", 
  "MANAGE_JOBAPPLICATIONS", 
  "MANAGE_JOBS", 
  "MANAGE_NEWS", 
  "MANAGE_ORDERS", 
  "MANAGE_PRODUCTS" ,
  "MANAGE_NOTIFICATIONS"
];

export default function AdminUsers() {
  const [activeTab, setActiveTab] = useState('allocate');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);

  // Search & Sort States
  const [searchTerm, setSearchTerm] = useState('');
  const [sortConfig, setSortConfig] = useState({ key: null, direction: 'asc' });

  // Role Management State
  const [roleForm, setRoleForm] = useState({ name: '', permissions: [] });
  const [editingRole, setEditingRole] = useState(null); 
  
  // User Allocation State
  const [editingUser, setEditingUser] = useState(null); 
  const [userRoleSelection, setUserRoleSelection] = useState([]);

  // Create User State 
  const [isCreateUserModalOpen, setIsCreateUserModalOpen] = useState(false);
  const [newUserForm, setNewUserForm] = useState({ name: '', email: '', password: '' });
  const [isCreatingUser, setIsCreatingUser] = useState(false);
  const [modalError, setModalError] = useState('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const [rolesRes, usersRes] = await Promise.all([ api.get('/api/roles'), api.get('/api/users') ]);
      setRoles(rolesRes.data.data || rolesRes.data || []);
      setUsers(usersRes.data.data || usersRes.data || []);
    } catch (err) { setError("Failed to load users or roles."); } finally { setLoading(false); }
  };

  useEffect(() => { fetchData(); }, []);

  // Search & Sort Logic
  const handleSort = (key) => {
    if (sortConfig.key === key) {
      if (sortConfig.direction === 'desc') {
        setSortConfig({ key, direction: 'asc' });
      } else if (sortConfig.direction === 'asc') {
        setSortConfig({ key: null, direction: null });
      }
    } else {
      setSortConfig({ key, direction: 'desc' });
    }
  };

  const getSortIcon = (columnName) => {
    if (sortConfig.key !== columnName || !sortConfig.key) return <ArrowUpDown className="w-4 h-4 ml-1 opacity-50" />;
    return sortConfig.direction === 'asc' ? <ArrowUp className="w-4 h-4 ml-1" /> : <ArrowDown className="w-4 h-4 ml-1" />;
  };

  const filteredAndSortedUsers = [...users]
    .filter(u => u.email?.toLowerCase().includes(searchTerm.toLowerCase()) || u.name?.toLowerCase().includes(searchTerm.toLowerCase()))
    .sort((a, b) => {
      if (!sortConfig.key) return 0;
      let aValue = a[sortConfig.key] || '';
      let bValue = b[sortConfig.key] || '';
      if (aValue.toLowerCase() < bValue.toLowerCase()) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aValue.toLowerCase() > bValue.toLowerCase()) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });

  // Create User Handlers
  const handleCreateUserClick = () => {
    setError(''); setSuccess(''); setModalError('');
    setNewUserForm({ name: '', email: '', password: '' }); 
    setIsCreateUserModalOpen(true);
  };

  const handleCreateUserSubmit = async (e) => {
    e.preventDefault();
    setIsCreatingUser(true);
    setError(''); setSuccess(''); setModalError('');
    
    try {
      await api.post('/api/auth/register', newUserForm);
      setSuccess("User created successfully! Please assign roles below.");
      setIsCreateUserModalOpen(false);

      const usersRes = await api.get('/api/users');
      const updatedUsers = usersRes.data.data || usersRes.data || [];
      setUsers(updatedUsers);

      const newlyCreatedUser = updatedUsers.find(u => u.email === newUserForm.email);
      if (newlyCreatedUser) {
        handleEditUserClick(newlyCreatedUser);
      }

      setNewUserForm({ name: '', email: '', password: '' }); 
    } catch (err) {
      console.error(err);
      setModalError(err.response?.data?.message || "Failed to create user. Email might already exist.");
    } finally {
      setIsCreatingUser(false);
    }
  };

  // Role Handlers
  const handlePermissionToggle = (perm) => { setRoleForm((prev) => ({ ...prev, permissions: prev.permissions.includes(perm) ? prev.permissions.filter(p => p !== perm) : [...prev.permissions, perm] })); };
  const handleEditRoleClick = (role) => { setEditingRole(role); setRoleForm({ name: role.name, permissions: role.permissions || [] }); window.scrollTo({ top: 0, behavior: 'smooth' }); };
  const handleCancelRoleEdit = () => { setEditingRole(null); setRoleForm({ name: '', permissions: [] }); setError(''); setSuccess(''); };
  const handleSaveRole = async (e) => {
    e.preventDefault(); setError(''); setSuccess('');
    if (roleForm.permissions.length === 0) return setError("Select at least one permission.");
    try {
      if (editingRole) await api.put(`/api/roles/${editingRole.name}/permissions`, roleForm.permissions);
      else await api.post('/api/roles', roleForm);
      setSuccess("Saved successfully!"); handleCancelRoleEdit(); fetchData(); 
    } catch (err) { setError("Failed to save role."); }
  };
  const handleDeleteRole = async (roleName) => { if(window.confirm("Delete role?")) { try { await api.delete(`/api/roles/${roleName}`); fetchData(); } catch (err) { setError("Failed to delete."); } } };
  
  // User Allocation Handlers
 const handleEditUserClick = (user) => { 
    setEditingUser(user); 
    
    setUserRoleSelection(user.roles ? user.roles.filter(r => r && r.name).map(r => r.name) : []); 
  };
  const handleUserRoleToggle = (roleName) => { setUserRoleSelection((prev) => prev.includes(roleName) ? prev.filter(r => r !== roleName) : [...prev, roleName]); };
  const handleCancelUserEdit = () => { setEditingUser(null); setUserRoleSelection([]); };
  const handleSaveUserRoles = async (e) => {
    e.preventDefault();
    try { await api.put('/api/roles/allocate', { email: editingUser.email, roleNames: userRoleSelection }); setSuccess("Roles updated successfully!"); handleCancelUserEdit(); fetchData(); } 
    catch (err) { setError("Failed to update user roles."); }
  };
  const handleDeleteUser = async (userId) => { if(window.confirm("Delete user?")) { try { await api.delete(`/api/users/${userId}`); fetchData(); } catch(err) { setError("Failed to delete."); } } };

  return (
    <div className="p-6">
      
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-gray-800">User & Role Management</h1>
        
        {activeTab === 'allocate' && (
          <button 
            onClick={handleCreateUserClick} 
            className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2"
          >
            + Create New User
          </button>
        )}
      </div>

      {error && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem]">{error}</p>}
      {success && <p className="mb-4 text-green-700 bg-green-50 p-3 rounded text-[0.85rem]">{success}</p>}

      <div className="flex border-b border-gray-100 mb-6 bg-white sticky top-0 z-10">
        <button onClick={() => { setActiveTab('allocate'); handleCancelRoleEdit(); }} className={`py-3 px-6 font-medium text-[0.85rem] border-b-2 ${activeTab === 'allocate' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:bg-gray-50'}`}>Manage Users</button>
        <button onClick={() => { setActiveTab('roles'); handleCancelUserEdit(); }} className={`py-3 px-6 font-medium text-[0.85rem] border-b-2 ${activeTab === 'roles' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:bg-gray-50'}`}>Manage Roles</button>
      </div>

      {loading ? ( <div className="p-8 text-center text-gray-500 text-[0.85rem]">Loading data...</div> ) : (
        <div className="flex-1 bg-white rounded shadow-sm overflow-hidden flex flex-col">
          
          {/* TAB 1: MANAGE USERS */}
          {activeTab === 'allocate' && (
            <div className="flex flex-col h-full border border-gray-100 rounded bg-white shadow-sm">
              <div className="p-4 border-b border-gray-100 flex items-center bg-white">
                <div className="relative flex-1 max-w-md">
                  <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none"><Search className="w-4 h-4 text-gray-400" /></div>
                  <input type="text" placeholder="Search users by name or email..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="w-full text-[0.85rem] px-3 py-2 pl-9 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" />
                </div>
                {searchTerm && <button onClick={() => setSearchTerm('')} className="ml-3 text-gray-500 hover:text-red-500 flex items-center text-[0.85rem]"><FilterX className="w-4 h-4 mr-1"/> Clear</button>}
              </div>

              <div className="flex-1 overflow-auto">
                <table className="w-full text-left border-collapse">
                  <thead className="bg-gray-50 border-b border-gray-100 sticky top-0 select-none">
                    <tr>
                      <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Name</th>
                      <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => handleSort('email')}>
                        <div className="flex items-center">User Email {getSortIcon('email')}</div>
                      </th>
                      <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider">Assigned Roles</th>
                      <th className="text-[0.75rem] uppercase text-gray-500 font-semibold px-4 py-3 text-left tracking-wider w-48">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredAndSortedUsers.length === 0 ? (
                      <tr><td colSpan="4" className="px-4 py-8 text-center text-gray-500 text-[0.85rem]">No users found.</td></tr>
                    ) : (
                      filteredAndSortedUsers.map((user) => (
                        <tr key={user.id || user._id} className="border-b border-gray-50 hover:bg-gray-50/50">
                          <td className="px-4 py-3 text-[0.85rem] text-gray-700 font-medium">{user.name || '-'}</td>
                          <td className="px-4 py-3 text-[0.85rem] text-gray-700">{user.email}</td>
                          <td className="px-4 py-3">
                            <div className="flex flex-wrap gap-2">
                              {user.roles && user.roles.map((r, i) => r && r.name ? (
                                <span key={i} className="px-2 py-0.5 text-[0.75rem] font-medium text-blue-700 bg-blue-50 border border-blue-100 rounded-full">
                                  {r.name}
                                </span>
                              ) : null)}
                              {(!user.roles || user.roles.length === 0) && <span className="text-[0.85rem] text-gray-400 italic">No roles assigned</span>}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex gap-2">
                              <button onClick={() => handleEditUserClick(user)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 transition-colors">Edit Roles</button>
                              <button onClick={() => handleDeleteUser(user.id || user._id)} className="bg-white border border-red-200 text-red-600 hover:bg-red-50 rounded text-[0.85rem] px-3 py-1.5 transition-colors">Delete</button>
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 2: MANAGE ROLES */}
          {activeTab === 'roles' && (
            <div className="p-6 flex flex-col md:flex-row gap-8">
              <div className="w-full md:w-1/2 bg-white p-4 rounded border border-gray-100 shadow-sm">
                <h3 className="font-semibold text-gray-800 text-[0.85rem] mb-4 border-b border-gray-100 pb-2">Available Roles</h3>
                {roles.length === 0 ? (
                  <p className="text-[0.85rem] text-gray-500">No roles defined yet.</p>
                ) : (
                  <ul className="space-y-3">
                    {roles.map((role) => (
                      <li key={role.name} className="p-4 bg-white border border-gray-100 rounded flex flex-col gap-3">
                        <div className="flex justify-between items-center">
                          <span className="font-medium text-gray-800 text-[0.85rem] px-2 py-1 bg-gray-50 rounded border border-gray-100">{role.name}</span>
                          <div className="flex gap-2">
                            {role.name !== 'SUPER_ADMIN' && (
                              <>
                                <button onClick={() => handleEditRoleClick(role)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-3 py-1.5 transition-colors">Edit</button>
                                <button onClick={() => handleDeleteRole(role.name)} className="bg-white border border-red-200 text-red-600 hover:bg-red-50 rounded text-[0.85rem] px-3 py-1.5 transition-colors">Delete</button>
                              </>
                            )}
                          </div>
                        </div>
                        <div className="text-[0.75rem] text-gray-500 leading-relaxed">{(role.permissions || []).join(', ') || 'No permissions'}</div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <div className="w-full md:w-1/2 bg-white p-6 border border-gray-100 rounded shadow-sm self-start sticky top-24">
                <h2 className="text-lg font-semibold text-gray-800 mb-4">{editingRole ? `Update Role: ${editingRole.name}` : 'Create New Role'}</h2>
                <form onSubmit={handleSaveRole} className="space-y-6">
                  <div>
                    <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Role Name *</label>
                    <input type="text" required disabled={editingRole !== null} value={roleForm.name} onChange={(e) => setRoleForm({ ...roleForm, name: e.target.value.toUpperCase().replace(/\s+/g, '_') })} className={`w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white uppercase ${editingRole ? 'bg-gray-50 text-gray-400 cursor-not-allowed' : ''}`} placeholder="e.g. EDITOR" />
                  </div>
                  <div>
                    <label className="block mb-3 text-[0.85rem] font-medium text-gray-700">Select Permissions *</label>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-4 border border-gray-200 rounded bg-gray-50 max-h-[400px] overflow-y-auto">
                      {AVAILABLE_PERMISSIONS.map((perm) => (
                        <label key={perm} className="flex items-center space-x-3 cursor-pointer p-2 hover:bg-gray-100 rounded">
                          <input type="checkbox" checked={roleForm.permissions.includes(perm)} onChange={() => handlePermissionToggle(perm)} className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500" />
                          <span className="text-[0.85rem] text-gray-700 truncate">{perm.replace(/_/g, ' ')}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                  <div className="flex gap-3 pt-2">
                    {editingRole && <button type="button" onClick={handleCancelRoleEdit} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-4 py-2 flex-1 transition-colors">Cancel</button>}
                    <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 flex-1">{editingRole ? 'Update Permissions' : 'Create Role'}</button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* CREATE USER MODAL */}
      {isCreateUserModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded shadow-xl overflow-hidden flex flex-col">
            <div className="p-4 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-lg font-semibold text-gray-800">Create New User</h2>
              <button onClick={() => setIsCreateUserModalOpen(false)} className="text-gray-400 hover:text-red-500 transition-colors"><X className="w-5 h-5" /></button>
            </div>
            
            <form onSubmit={handleCreateUserSubmit} className="p-6">
              {modalError && <p className="mb-4 text-red-500 bg-red-50 p-3 rounded text-[0.85rem]">{modalError}</p>}
              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Full Name <span className="text-red-500">*</span></label>
                <input 
                  type="text" 
                  required 
                  value={newUserForm.name} 
                  onChange={(e) => setNewUserForm({ ...newUserForm, name: e.target.value })} 
                  className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                  placeholder="e.g. John Doe" 
                />
              </div>

              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Email Address <span className="text-red-500">*</span></label>
                <input 
                  type="email" 
                  required 
                  value={newUserForm.email} 
                  onChange={(e) => setNewUserForm({ ...newUserForm, email: e.target.value })} 
                  className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                  placeholder="e.g. admin@laserxprts.com" 
                />
              </div>

              <div className="mb-6">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">Password <span className="text-red-500">*</span></label>
                <input 
                  type="text" 
                  required 
                  value={newUserForm.password} 
                  onChange={(e) => setNewUserForm({ ...newUserForm, password: e.target.value })} 
                  className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded focus:ring-0 focus:border-blue-400 outline-none bg-white" 
                  placeholder="Enter a strong password" 
                />
                <p className="text-[0.75rem] text-gray-500 mt-2">Note: Password is shown in plain text so you can easily copy and share it with the user securely.</p>
              </div>

              <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
                <button type="button" onClick={() => setIsCreateUserModalOpen(false)} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-4 py-2 transition-colors">Cancel</button>
                <button type="submit" disabled={isCreatingUser} className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2 disabled:opacity-50">
                  {isCreatingUser ? 'Creating...' : 'Create User & Assign Role'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT USER ROLES MODAL (Allocation Modal) */}
      {editingUser && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="w-full max-w-md bg-white rounded shadow-xl overflow-hidden">
            <div className="p-4 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-lg font-semibold text-gray-800">Assign Roles to User</h2>
              <button onClick={handleCancelUserEdit} className="text-gray-400 hover:text-red-500 transition-colors"><X className="w-5 h-5" /></button>
            </div>
            <form onSubmit={handleSaveUserRoles} className="p-6">
              <div className="mb-4">
                <label className="block mb-1 text-[0.85rem] font-medium text-gray-700">User Email</label>
                <input type="text" disabled value={editingUser.email} className="w-full text-[0.85rem] px-3 py-2 border border-gray-200 rounded bg-gray-50 text-gray-500 outline-none" />
              </div>
              <div className="mb-6">
                <label className="block mb-2 text-[0.85rem] font-medium text-gray-700">Select Roles to Assign</label>
                <div className="space-y-2 max-h-48 overflow-y-auto p-3 border border-gray-200 rounded bg-gray-50">
                  {roles.map(role => (
                    <label key={role.name} className="flex items-center space-x-3 cursor-pointer p-2 hover:bg-gray-100 rounded transition-colors">
                      <input type="checkbox" checked={userRoleSelection.includes(role.name)} onChange={() => handleUserRoleToggle(role.name)} className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500" />
                      <span className="text-[0.85rem] text-gray-700">{role.name}</span>
                    </label>
                  ))}
                  {roles.length === 0 && <p className="text-[0.85rem] text-gray-500 italic">No roles created yet.</p>}
                </div>
              </div>
              <div className="flex justify-end space-x-3 mt-4 border-t border-gray-100 pt-4">
                <button type="button" onClick={handleCancelUserEdit} className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 rounded text-[0.85rem] px-4 py-2 transition-colors">Cancel</button>
                <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white rounded text-[0.85rem] font-medium transition-colors shadow-sm px-4 py-2">Save Roles</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}