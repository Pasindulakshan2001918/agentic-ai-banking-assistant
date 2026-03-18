import EnterpriseLayout from "../components/EnterpriseLayout";

export default function ApproverDashboard() {
  return (
    <EnterpriseLayout title="APPROVER – Transaction Authorization">

      <div className="grid md:grid-cols-2 gap-6">

        <div className="p-6 bg-yellow-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Pending Approvals</h3>
          <p>Review and approve pending transactions.</p>
        </div>

        <div className="p-6 bg-red-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Risk Flags</h3>
          <p>Investigate AML alerts.</p>
        </div>

        <div className="p-6 bg-indigo-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Approval History</h3>
          <p>View previously approved transactions.</p>
        </div>

      </div>
    </EnterpriseLayout>
  );
}