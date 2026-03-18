import EnterpriseLayout from "../components/EnterpriseLayout";

export default function CreatorDashboard() {
  return (
    <EnterpriseLayout title="CREATOR – Transaction Initiation">

      <div className="grid md:grid-cols-2 gap-6">

        <div className="p-6 bg-green-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Create Transaction</h3>
          <p>Initiate new transfer requests.</p>
        </div>

        <div className="p-6 bg-indigo-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">My Transactions</h3>
          <p>Track approval status.</p>
        </div>

        <div className="p-6 bg-yellow-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Pending Approvals</h3>
          <p>Transactions awaiting checker approval.</p>
        </div>

        <div className="p-6 bg-purple-800/30 rounded-2xl">
          <h3 className="text-xl font-semibold mb-2">Transaction Statistics</h3>
          <p>Daily summary and totals.</p>
        </div>

      </div>
    </EnterpriseLayout>
  );
}