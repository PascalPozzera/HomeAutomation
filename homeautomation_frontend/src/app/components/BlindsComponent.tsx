import { useState, useEffect, useRef } from "react";

export default function BlindsComponent() {
    const [open, setOpen] = useState(false);
    const [isOpen, setIsOpen] = useState<boolean | null>(null);
    const modalRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const interval = setInterval(fetchStatus, 2000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (modalRef.current && !modalRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };

        if (open) {
            document.addEventListener("mousedown", handleClickOutside);
        }

        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [open]);

    const fetchStatus = async () => {
        try {
            const res = await fetch("/api/blinds/status");
            const data = await res.json();
            setIsOpen(data);
        } catch (err) {
            console.error("Failed to fetch blinds status", err);
        }
    };

    const openBlinds = async () => {
        await fetch("/api/blinds/position?open=true", { method: "POST" });
        await fetchStatus();
    };

    const closeBlinds = async () => {
        await fetch("/api/blinds/position?open=false", { method: "POST" });
        await fetchStatus();
    };

    return (
        <>
            <div
                onClick={() => setOpen(true)}
                className="relative bg-white shadow-md rounded-xl p-4 text-center cursor-pointer hover:shadow-lg transition"
            >
                <img src="/blinds-icon.png" alt="Blinds" className="w-16 h-16 mx-auto mb-2" />
                <h2 className="text-lg font-semibold text-gray-800">Blinds</h2>
                <p className="text-sm text-gray-500">{isOpen ? "Open" : "Closed"}</p>
            </div>

            {open && (
                <div className="fixed inset-0 backdrop-blur-sm bg-white/20 flex items-center justify-center z-50">
                    <div
                        ref={modalRef}
                        className="bg-white rounded-xl p-6 w-96 shadow-2xl"
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
                                        {isOpen ? "Open" : "Closed"}
                                    </strong>
                                </p>
                            ) : (
                                <p className="text-sm text-gray-400">Loading status...</p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <button
                                onClick={openBlinds}
                                className={`w-full py-2 rounded-lg ${
                                    isOpen
                                        ? "bg-blue-500 text-white"
                                        : "bg-gray-200 text-black hover:bg-gray-300"
                                }`}
                            >
                                Open Blinds
                            </button>
                            <button
                                onClick={closeBlinds}
                                className={`w-full py-2 rounded-lg ${
                                    isOpen === false
                                        ? "bg-blue-500 text-white"
                                        : "bg-gray-200 text-black hover:bg-gray-300"
                                }`}
                            >
                                Close Blinds
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
