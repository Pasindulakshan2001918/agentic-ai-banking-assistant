import EnterpriseLayout from "../components/EnterpriseLayout";

export default function ViewerDashboard() {
  return (
    <EnterpriseLayout title="VIEWER – Audit & Monitoring">

      <div className="grid md:grid-cols-2 gap-6">

        <div className="p-6 bg-gray-800/40 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Audit Logs</h3>
          <p>View system activity logs.</p>
        </div>

        <div className="p-6 bg-indigo-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Reports</h3>
          <p>Daily transaction and compliance reports.</p>
        </div>

      </div>
    </EnterpriseLayout>
  );
}