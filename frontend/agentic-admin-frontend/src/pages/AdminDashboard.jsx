import EnterpriseLayout from "../components/EnterpriseLayout";

export default function AdminDashboard() {
  return (
    <EnterpriseLayout title="ADMIN – Operations Management">

      <div className="grid md:grid-cols-2 gap-6">

        <div className="p-6 bg-blue-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">User Management</h3>
          <p>Create, update, suspend banking users.</p>
        </div>

        <div className="p-6 bg-green-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Transaction Limits</h3>
          <p>Configure daily transfer thresholds.</p>
        </div>

        <div className="p-6 bg-yellow-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Branch & ATM Setup</h3>
          <p>Manage branch locations and ATM networks.</p>
        </div>

        <div className="p-6 bg-indigo-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Service Configuration</h3>
          <p>Enable bill payments, QR services, and features.</p>
        </div>

      </div>
    </EnterpriseLayout>
  );
}