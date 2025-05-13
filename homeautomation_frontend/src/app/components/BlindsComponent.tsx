import {useState, useEffect} from "react";

export default function BlindsComponent() {
    const [open, setOpen] = useState(false);
    const [isOpen, setIsOpen] = useState<boolean | null>(null);

    useEffect(() => {
        const interval = setInterval(fetchStatus, 2000);
        return () => clearInterval(interval);
    }, []);

    const fetchStatus = async () => {
        try {
            const res = await fetch("/api/blinds/status");
            const data = await res.json();
            console.log("Blinds status:", data); // 🔍 Log
            setIsOpen(data);
        } catch (err) {
            console.error("Failed to fetch blinds status", err);
        }
    };


    const openBlinds = async () => {
        await fetch("/api/blinds/position?open=true", {method: "POST"});
        await fetchStatus();
    };

    const closeBlinds = async () => {
        await fetch("/api/blinds/position?open=false", {method: "POST"});
        await fetchStatus();
    };

    return (
        <>
            {/* Blinds Box */}
            <div
                onClick={() => setOpen(true)}
                className="relative bg-white shadow-md rounded-xl p-4 text-center cursor-pointer hover:shadow-lg transition"
            >
                <img src="/blinds-icon.png" alt="Blinds" className="w-16 h-16 mx-auto mb-2"/>
                <h2 className="text-lg font-semibold text-gray-800">Blinds</h2>
                <p className="text-sm text-gray-500">{isOpen ? "Open" : "Closed"}</p>
            </div>

            {/* Modal */}
            {open && (
                <div
                    className="fixed inset-0 backdrop-blur-sm bg-white/20 flex items-center justify-center z-50"
                    onClick={() => setOpen(false)}
                >
                    <div
                        className="bg-white rounded-xl p-6 w-96 shadow-2xl"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-xl font-bold text-gray-800">Blinds Control</h3>
                            <button
                                onClick={() => setOpen(false)}
                                className="text-gray-500 hover:text-black text-2xl"
                            >
                                &times;
                            </button>
                        </div>

                        <div className="text-center text-gray-700 mb-4">
                            {isOpen !== null ? (
                                <p className="text-lg">
                                    Current state:{" "}
                                    <strong className={isOpen ? "text-green-600" : "text-red-600"}>
                                        <p className="text-sm text-gray-500">
                                            {isOpen === null ? "Loading..." : isOpen ? "Open" : "Closed"}
                                        </p>

                                    </strong>
                                </p>
                            ) : (
                                <p className="text-sm text-gray-400">Loading status...</p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <button
                                onClick={openBlinds}
                                className="w-full bg-blue-500 text-white py-2 rounded-lg hover:bg-blue-600"
                            >
                                🪟 Open Blinds
                            </button>
                            <button
                                onClick={closeBlinds}
                                className="w-full bg-gray-200 text-black py-2 rounded-lg hover:bg-gray-300"
                            >
                                🧱 Close Blinds
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
