import { useState, useEffect } from "react";

export default function FridgeComponent() {
    const [open, setOpen] = useState(false);
    const [contents, setContents] = useState<any>(null);
    const [history, setHistory] = useState<any>([]);
    const [receipts, setReceipts] = useState<any>([]);
    const [loading, setLoading] = useState(false);
    const [order, setOrder] = useState({ name: "", price: 0, weight: 0, quantity: 1 });

    useEffect(() => {
        fetchContents();
        fetchHistory();
        fetchReceipts();
    }, []);

    const fetchContents = async () => {
        setLoading(true);
        const res = await fetch("/api/fridge/contents");
        const data = await res.json();
        setContents(data);
        setLoading(false);
    };

    const fetchHistory = async () => {
        const res = await fetch("/api/fridge/history");
        const data = await res.json();
        setHistory(data.orders || []);
    };

    const fetchReceipts = async () => {
        const res = await fetch("/api/fridge/receipts");
        const data = await res.json();
        setReceipts(data);
    };

    const consume = async (id: string, quantity: number) => {
        await fetch(`/api/fridge/consume?id=${id}&quantity=${quantity}`, { method: "POST" });
        await fetchContents();
    };

    const submitOrder = async () => {
        const { name, price, weight, quantity } = order;

        if (!name || price <= 0 || weight <= 0 || quantity <= 0) {
            alert("Please fill out all fields correctly.");
            return;
        }

        await fetch(
            `/api/fridge/order?name=${encodeURIComponent(name)}&price=${price}&weight=${weight}&quantity=${quantity}`,
            { method: "POST" }
        );

        setOrder({ name: "", price: 0, weight: 0, quantity: 1 });
        await fetchContents();
        await fetchHistory();
        await fetchReceipts();
    };

    return (
        <>
            <div
                onClick={() => setOpen(true)}
                className="relative bg-white shadow-md rounded-xl p-4 text-center cursor-pointer hover:shadow-lg transition"
            >
                <img src="/fridge-icon.png" alt="Fridge" className="w-16 h-16 mx-auto mb-2" />
                <h2 className="text-lg font-semibold text-gray-800">Fridge</h2>
                <p className="text-sm text-gray-500">
                    {contents
                        ? `${contents.currentItemCount}/${contents.maxItemCount} items, ${contents.currentWeight.toFixed(1)}kg`
                        : "Loading..."}
                </p>
            </div>

            {open && (
                <div
                    className="fixed inset-0 backdrop-blur-sm bg-white/20 flex items-center justify-center z-50"
                    onClick={() => setOpen(false)}
                >
                    <div
                        className="bg-white rounded-xl p-6 w-[45rem] max-h-[90vh] overflow-y-auto shadow-2xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-xl font-bold text-gray-800">Fridge Overview</h3>
                            <button
                                onClick={() => setOpen(false)}
                                className="text-gray-500 hover:text-black text-2xl"
                            >
                                &times;
                            </button>
                        </div>

                        {loading ? (
                            <p className="text-center text-gray-500">Loading...</p>
                        ) : (
                            <>
                                <p className="mb-4 text-sm text-gray-600">
                                    Items: <strong>{contents.currentItemCount}</strong> / {contents.maxItemCount} &nbsp;
                                    | &nbsp; Weight: <strong>{contents.currentWeight.toFixed(1)}kg</strong> / {contents.maxWeight}kg
                                </p>

                                <div className="grid grid-cols-2 gap-4 mb-6">
                                    {contents.products.map((product: any) => (
                                        <div key={product.id} className="border p-3 rounded-lg shadow-sm">
                                            <div className="font-semibold text-gray-800">{product.name}</div>
                                            <div className="text-sm text-gray-500">
                                                {product.quantity}x • {product.weight}kg • {product.price}€
                                            </div>
                                            <button
                                                onClick={() => consume(product.id, 1)}
                                                className="mt-2 text-sm bg-red-100 text-red-700 px-3 py-1 rounded hover:bg-red-200"
                                            >
                                                Consume 1
                                            </button>
                                        </div>
                                    ))}
                                </div>

                                <hr className="my-4" />
                                <h4 className="text-lg font-semibold mb-2 text-gray-700">Order Product</h4>
                                <div className="space-y-2 text-sm mb-6">
                                    <input
                                        type="text"
                                        placeholder="Product name"
                                        className="w-full border p-2 rounded"
                                        value={order.name}
                                        onChange={(e) => setOrder({ ...order, name: e.target.value })}
                                    />
                                    <input
                                        type="number"
                                        placeholder="Price (€)"
                                        className="w-full border p-2 rounded"
                                        value={order.price}
                                        onChange={(e) => setOrder({ ...order, price: parseFloat(e.target.value) })}
                                    />
                                    <input
                                        type="number"
                                        placeholder="Weight (kg)"
                                        className="w-full border p-2 rounded"
                                        value={order.weight}
                                        onChange={(e) => setOrder({ ...order, weight: parseFloat(e.target.value) })}
                                    />
                                    <input
                                        type="number"
                                        placeholder="Quantity"
                                        className="w-full border p-2 rounded"
                                        value={order.quantity}
                                        onChange={(e) => setOrder({ ...order, quantity: parseInt(e.target.value) })}
                                    />
                                    <button
                                        onClick={submitOrder}
                                        className="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600 w-full"
                                    >
                                        Order
                                    </button>
                                </div>

                                <hr className="my-4" />
                                <h4 className="text-lg font-semibold mb-2 text-gray-700">Order History</h4>
                                {history.length > 0 ? (
                                    <div className="space-y-4 mb-6">
                                        {history.map((order: any) => (
                                            <div key={order.id} className="border rounded p-3 bg-gray-50">
                                                <div className="text-sm text-gray-600 mb-1">
                                                    {order.timestamp} • Total: {order.totalPrice}€
                                                </div>
                                                <ul className="text-sm list-disc pl-4 text-gray-800">
                                                    {order.items.map((item: any, index: number) => (
                                                        <li key={index}>
                                                            {item.quantity}x {item.name} ({item.price}€)
                                                        </li>
                                                    ))}
                                                </ul>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-sm text-gray-500">No orders yet.</p>
                                )}

                                <hr className="my-4" />
                                <h4 className="text-lg font-semibold mb-2 text-gray-700">Receipts</h4>
                                {receipts.length > 0 ? (
                                    <div className="space-y-4">
                                        {receipts.map((r: any, idx: number) => (
                                            <div key={idx} className="border rounded p-3 text-sm bg-white shadow-sm">
                                                <div className="text-gray-600 mb-1">
                                                    ID: <strong>{r.id}</strong> • {r.timestamp}
                                                </div>
                                                <ul className="list-disc pl-4 text-gray-800 mb-1">
                                                    {r.items.map((item: any, i: number) => (
                                                        <li key={i}>
                                                            {item.quantity}x {item.name} • {item.price}€
                                                        </li>
                                                    ))}
                                                </ul>
                                                <div className="text-right font-semibold">Total: {r.total}€</div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-sm text-gray-500">No receipts yet.</p>
                                )}
                            </>
                        )}
                    </div>
                </div>
            )}
        </>
    );
}
