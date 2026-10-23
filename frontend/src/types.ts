/** UUIDs and ISO dates are strings at the HTTP boundary. Names mirror the Java projections. */
export type Id = string;
export type Language = 'en' | 'de';
export type Role = 'ADMIN' | 'SUPERVISOR' | 'INSPECTOR' | 'TECHNICIAN' | 'VIEWER';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export interface User { id: Id; organizationId?: Id; email?: string; displayName: string; role: Role; active?: boolean }
export interface Organization { id: Id; name: string; timezone: string }
export interface Site { id: Id; name: string; code: string; address: string }
export interface Asset {
  id: Id; siteId: Id; code: string; name: string; category: string; manufacturer: string;
  model: string; serialNumber: string; locationNote: string; criticality: Severity; archived: boolean; createdAt: string;
}
export interface Template { id: Id; name: string; nameDe: string; description: string; version: number; status: 'DRAFT' | 'REVIEWED'; reviewedBy: Id | null; reviewedAt: string | null; createdAt: string }
export interface TemplateItem { id: Id; templateId: Id; position: number; label: string; labelDe: string; kind: 'PASS_FAIL' | 'NUMBER' | 'TEXT'; required: boolean; minValue: number | null; maxValue: number | null; unit: string }
export interface Schedule { id: Id; assetId: Id; templateId: Id; ownerId: Id; intervalDays: number; nextDue: string; reason: string; reviewedBy: Id; active: boolean; createdAt: string }
export interface Inspection { id: Id; assetId: Id; templateId: Id; scheduleId: Id | null; inspectorId: Id; status: 'DRAFT' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED'; plannedDue: string | null; performedAt: string | null; submittedAt: string | null; reviewedBy: Id | null; reviewNote: string; version: number; createdAt: string; requestKey?: string }
export interface Answer { inspectionId: Id; itemId: Id; value: string; note: string; outcome: 'PASS' | 'FAIL' | 'NA' }
export interface Finding {
  id: Id; assetId: Id; inspectionId: Id; itemId: Id; title: string; description: string; severity: Severity;
  assigneeId: Id | null; dueDate: string | null; status: 'OPEN' | 'IN_PROGRESS' | 'AWAITING_VERIFICATION' | 'CLOSED';
  repairNote: string; completedBy: Id | null; verifiedBy: Id | null; verifyNote: string; resolvedAt: string | null; version: number; createdAt: string;
}
export interface Evidence { id: Id; assetId: Id; inspectionId: Id | null; findingId: Id | null; uploadedBy: Id; filename: string; mime: string; size: number; sha256: string; createdAt: string; previewData?: string }
export interface AuditEvent { id: Id; actorId: Id; actorName: string; entityType: string; entityId: Id; action: string; detail: string; occurredAt: string }
export interface Workspace {
  organization: Organization; user: User; demo: boolean; today: string; serverNow: string;
  sites: Site[]; assets: Asset[]; templates: Template[]; templateItems: TemplateItem[];
  schedules: Schedule[]; inspections: Inspection[]; answers: Answer[]; findings: Finding[];
  evidence: Evidence[]; users: User[]; audit: AuditEvent[]; historyTruncated: boolean;
}
export interface Passport { asset: Asset; inspections: Inspection[]; answers: Answer[]; findings: Finding[]; evidence: Evidence[]; audit: AuditEvent[]; page: number; hasMore: boolean }
export interface Api {
  readonly preview: boolean;
  get<T>(path: string): Promise<T>;
  send<T>(method: 'POST' | 'PUT' | 'PATCH', path: string, body?: unknown): Promise<T>;
  upload(path: string, file: File): Promise<Evidence>;
  login(email: string, password: string): Promise<void>;
  logout(): Promise<void>;
  download(path: string): Promise<Blob>;
}
