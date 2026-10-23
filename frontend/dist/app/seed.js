import { addDays } from './domain.js';
const id = (n) => `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
export const demoPng = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jFZkAAAAASUVORK5CYII=';
/** Explicitly fictional sample data. No customer names, fabricated certifications or personal information. */
export function createDemoState(today = new Date().toISOString().slice(0, 10)) {
    const at = (days, hour = 10) => `${addDays(today, days)}T${String(hour).padStart(2, '0')}:00:00Z`;
    const users = [
        { id: id(1), displayName: 'Ahad Porkar', email: 'owner@pruefpilot.test', role: 'ADMIN', active: true },
        { id: id(2), displayName: 'Lena Fischer', email: 'supervisor@pruefpilot.test', role: 'SUPERVISOR', active: true },
        { id: id(3), displayName: 'Jonas Weber', email: 'inspector@pruefpilot.test', role: 'INSPECTOR', active: true },
        { id: id(4), displayName: 'Mara Klein', email: 'technician@pruefpilot.test', role: 'TECHNICIAN', active: true },
        { id: id(5), displayName: 'Customer review', email: 'viewer@pruefpilot.test', role: 'VIEWER', active: true },
    ];
    const sites = [
        { id: id(101), name: 'Berlin / Workshop', code: 'BER', address: 'Example location, Berlin, Germany' },
        { id: id(102), name: 'Hamburg / Logistics', code: 'HAM', address: 'Example location, Hamburg, Germany' },
        { id: id(103), name: 'Leipzig / Operations', code: 'LEJ', address: 'Example location, Leipzig, Germany' },
    ];
    const names = ['Hydraulic lift / Bay 01', 'Air compressor / Unit 07', 'Extraction system / Line 04', 'Mobile work platform / 02', 'Loading dock gate / A', 'Pallet truck / E-12', 'Warehouse ladder / 03', 'Ventilation unit / North', 'Bench grinder / Station 06', 'Service lift / B', 'Workshop trolley / 08', 'Safety cabinet / West'];
    const categories = ['Lifting equipment', 'Compressed air', 'Ventilation', 'Access equipment', 'Dock equipment', 'Material handling', 'Access equipment', 'Ventilation', 'Workshop equipment', 'Lifting equipment', 'Workshop equipment', 'Storage'];
    const assets = names.map((name, n) => ({ id: id(201 + n), siteId: sites[Math.floor(n / 4)].id, code: `PP-${sites[Math.floor(n / 4)].code}-${String(n + 1).padStart(3, '0')}`, name, category: categories[n], manufacturer: 'Example manufacturer', model: `Series ${100 + n}`, serialNumber: `DEMO-${4000 + n}`, locationNote: `Zone ${n % 4 + 1}`, criticality: n < 2 ? 'HIGH' : 'MEDIUM', archived: false, createdAt: at(-45) }));
    const templateId = id(301), labels = ['Asset identification is readable', 'Work area is clear and accessible', 'No visible damage or leakage', 'Operator observations'];
    const labelsDe = ['Anlagenkennzeichnung ist lesbar', 'Arbeitsbereich ist frei und zug\u00e4nglich', 'Keine sichtbaren Sch\u00e4den oder Undichtigkeiten', 'Beobachtungen der Bedienperson'];
    const items = labels.map((label, n) => ({ id: id(310 + n), templateId, position: n, label, labelDe: labelsDe[n], kind: n === 3 ? 'TEXT' : 'PASS_FAIL', required: n !== 3, minValue: null, maxValue: null, unit: '' }));
    const offsets = [-2, 0, -1, 23, 1, 2, 5, 7, 9, 14, 18, 24];
    const schedules = assets.map((asset, n) => ({ id: id(601 + n), assetId: asset.id, templateId, ownerId: id(3), intervalDays: 30, nextDue: addDays(today, offsets[n]), reason: 'DEMO ONLY: arbitrary operational interval. Replace after qualified review.', reviewedBy: id(2), active: true, createdAt: at(-30) }));
    const inspections = assets.slice(0, 5).map((asset, n) => ({ id: id(701 + n), assetId: asset.id, templateId, scheduleId: schedules[n].id, inspectorId: id(3), status: n === 4 ? 'DRAFT' : n === 3 ? 'VERIFIED' : 'SUBMITTED', plannedDue: n === 3 ? addDays(today, -7) : schedules[n].nextDue, performedAt: n === 4 ? null : at(n === 3 ? -7 : 0, 8 + n), submittedAt: n === 4 ? null : at(n === 3 ? -7 : 0, 9 + n), reviewedBy: n === 3 ? id(2) : null, reviewNote: n === 3 ? 'DEMO: recorded observations reviewed.' : '', version: n === 4 ? 0 : n === 3 ? 2 : 1, createdAt: at(n === 3 ? -7 : 0, 7 + n) }));
    const answers = inspections.slice(0, 4).flatMap((inspection, n) => items.map((item, pos) => ({ inspectionId: inspection.id, itemId: item.id, value: pos === 3 ? 'Demonstration inspection' : pos === 2 && n < 2 ? 'FAIL' : 'PASS', outcome: pos === 2 && n < 2 ? 'FAIL' : 'PASS', note: pos === 2 && n < 2 ? 'DEMO: visible seepage at a connection; investigate before use.' : '' })));
    const findings = inspections.slice(0, 2).map((inspection, n) => ({ id: id(801 + n), assetId: inspection.assetId, inspectionId: inspection.id, itemId: id(312), title: labels[2], description: n === 0 ? 'DEMO: visible seepage at the hydraulic connection. Investigate before use.' : 'DEMO: air connection shows leakage during operation.', severity: n === 0 ? 'HIGH' : 'MEDIUM', assigneeId: id(4), dueDate: addDays(today, n + 1), status: n === 0 ? 'IN_PROGRESS' : 'AWAITING_VERIFICATION', repairNote: n === 1 ? 'DEMO: connection replaced and checked. Attached image is only a placeholder, not real repair evidence.' : '', completedBy: n === 1 ? id(4) : null, verifiedBy: null, verifyNote: '', resolvedAt: null, version: n === 0 ? 2 : 3, createdAt: at(0, 8 + n) }));
    return {
        organization: { id: id(100), name: 'Nordwerk / Service operations', timezone: 'Europe/Berlin' }, user: users[0], demo: true, today, serverNow: at(0, 12),
        sites, assets, templates: [{ id: templateId, name: 'Workshop asset / visual check', nameDe: 'Werkstattanlage / Sichtkontrolle', description: 'DEMONSTRATION ONLY. Not a statutory inspection template. Qualified domain review required before real use.', version: 1, status: 'REVIEWED', reviewedBy: id(2), reviewedAt: at(-31), createdAt: at(-32) }], templateItems: items, schedules, inspections, answers, findings,
        evidence: [{ id: id(901), assetId: id(202), inspectionId: id(702), findingId: id(802), uploadedBy: id(4), filename: 'DEMO-placeholder-not-repair-evidence.png', mime: 'image/png', size: 68, sha256: '', createdAt: at(0, 11), previewData: demoPng }],
        users, audit: [
            { id: id(1001), actorId: id(4), actorName: 'Mara Klein', entityType: 'FINDING', entityId: id(802), action: 'REPAIR_DOCUMENTED', detail: 'DEMO: air compressor connection repair documented.', occurredAt: at(0, 11) },
            { id: id(1002), actorId: id(3), actorName: 'Jonas Weber', entityType: 'INSPECTION', entityId: id(703), action: 'INSPECTION_SUBMITTED', detail: 'Extraction system / Line 04', occurredAt: at(0, 10) },
            { id: id(1003), actorId: id(4), actorName: 'Mara Klein', entityType: 'FINDING', entityId: id(801), action: 'REPAIR_STARTED', detail: 'Hydraulic lift / Bay 01', occurredAt: at(0, 9) },
            { id: id(1004), actorId: id(2), actorName: 'Lena Fischer', entityType: 'INSPECTION', entityId: id(704), action: 'INSPECTION_VERIFIED', detail: 'DEMO: mobile work platform record reviewed.', occurredAt: at(-7, 12) },
        ], historyTruncated: false,
    };
}
//# sourceMappingURL=seed.js.map