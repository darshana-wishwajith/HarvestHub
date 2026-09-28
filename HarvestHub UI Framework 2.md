\# HarvestHub UI Design Framework



\## 1. Core Identity \& Assets

\* \*\*Theme:\*\* Modern Agricultural Management System.

\* \*\*Style:\*\* Clean, abstract, and data-focused. Avoid overly realistic or cartoonish illustrations.

\* \*\*Primary Font Family:\*\* Plus Jakarta Sans (across all platforms).

\* \*\*Official Logo \& Brand Identity Vector Assets:\*\* \[HarvestHub Google Drive Asset Folder](https://drive.google.com/file/d/1-SRSamIciZn19\_16CPQGzEyf9U3Z1mV1/view?usp=sharing)



\---



\## 2. Color System

Use these exact hex codes for all UI components. Maintain a clean, high-contrast aesthetic.



\### A. Brand \& Core UI Colors

| Role | Swatch | Hex Code | Usage Rules |

| :--- | :---: | :--- | :--- |

| \*\*Primary\*\* | !\[#1B4332](https://placehold.co/24x24/1B4332/1B4332.png) | `#1B4332` | App bars, primary action buttons (Save/Submit), active states. |

| \*\*Secondary\*\* | !\[#4CAF50](https://placehold.co/24x24/4CAF50/4CAF50.png) | `#4CAF50` | Floating Action Buttons (FABs), icons, active toggles. |

| \*\*Surface/Tint\*\* | !\[#E8F5EA](https://placehold.co/24x24/E8F5EA/E8F5EA.png) | `#E8F5EA` | Card backgrounds, alternating data table rows, selected items. |

| \*\*Background\*\* | !\[#FFFFFF](https://placehold.co/24x24/FFFFFF/CCCCCC.png) | `#FFFFFF` | Main application background canvas. \*(Border added for visibility)\* |

| \*\*Text (Primary)\*\* | !\[#1A1A1A](https://placehold.co/24x24/1A1A1A/1A1A1A.png) | `#1A1A1A` | All main headings and primary body text. |

| \*\*Text (Muted)\*\* | !\[#64748B](https://placehold.co/24x24/64748B/64748B.png) | `#64748B` | Subtitles, placeholder text, disabled UI elements. |



\### B. System Notifications \& Alerts (Alert Badges/Toasts)

| Type | Swatch | Hex Code | Meaning / Usage |

| :--- | :---: | :--- | :--- |

| \*\*Error / Critical\*\* | !\[#DC2626](https://placehold.co/24x24/DC2626/DC2626.png) | `#DC2626` | Destructive actions, validation failures, failed operations. |

| \*\*Warning / Alert\*\* | !\[#D97706](https://placehold.co/24x24/D97706/D97706.png) | `#D97706` | Non-blocking alerts, system warnings, configuration adjustments. |

| \*\*Info / Notification\*\*| !\[#2563EB](https://placehold.co/24x24/2563EB/2563EB.png) | `#2563EB` | Neutral updates, system logs, informational popups. |



\### C. Semantic Workflow Statuses (From HarvestHub System Specifications)

These colors must color-code workflow status badges dynamically across both Mobile and Desktop grids.



| System Workflow Status | Swatch | Hex Code | Color Mode | Associated Workflow State |

| :--- | :---: | :--- | :--- | :--- |

| \*\*Drafting\*\* | !\[#64748B](https://placehold.co/24x24/64748B/64748B.png) | `#64748B` | Muted Grey | Location-Locked Initiation / Smart Guided Capture |

| \*\*Pending / Escrow Setup\*\* | !\[#D97706](https://placehold.co/24x24/D97706/D97706.png) | `#D97706` | Amber Gold | Supply Request published / Waiting for Buyer commitment |

| \*\*Active / Harvest-Locked\*\*| !\[#2563EB](https://placehold.co/24x24/2563EB/2563EB.png) | `#2563EB` | Royal Blue | Financial Commitment Locked / Farmer harvesting timeline |

| \*\*In-Transit\*\* | !\[#7C3AED](https://placehold.co/24x24/7C3AED/7C3AED.png) | `#7C3AED` | Purple | Transporter picked up cargo / Handover validation active |

| \*\*Delivered / Success\*\* | !\[#16A34A](https://placehold.co/24x24/16A34A/16A34A.png) | `#16A34A` | Emerald Green| Buyer accepted cargo / Funds released successfully |

| \*\*Disputed\*\* | !\[#DC2626](https://placehold.co/24x24/DC2626/DC2626.png) | `#DC2626` | Crimson Red | Cargo rejected / Return Journey / Resolution flow |

| \*\*Closed\*\* | !\[#0F172A](https://placehold.co/24x24/0F172A/0F172A.png) | `#0F172A` | Deep Slate | Ratings complete / Transaction cycle finalized |



\---



\## 3. Typography Hierarchy

\*(Visual sizes are approximated using native Markdown rendering. Adhere to the exact pixel/sp values defined below).\*



<h1>Heading 1 (H1)</h1>

<ul>

&#x20; <li><strong>Desktop:</strong> Bold, 32px</li>

&#x20; <li><strong>Mobile:</strong> Bold, 28sp</li>

&#x20; <li><strong>Usage:</strong> Main Page / Screen Titles</li>

</ul>



<h2>Heading 2 (H2)</h2>

<ul>

&#x20; <li><strong>Desktop:</strong> SemiBold, 24px</li>

&#x20; <li><strong>Mobile:</strong> SemiBold, 22sp</li>

&#x20; <li><strong>Usage:</strong> Panel Headers, Section Titles</li>

</ul>



<h3>Heading 3 (H3)</h3>

<ul>

&#x20; <li><strong>Desktop:</strong> Medium, 18px</li>

&#x20; <li><strong>Mobile:</strong> Medium, 18sp</li>

&#x20; <li><strong>Usage:</strong> Table Headers, Card Titles, Status Badge Labels</li>

</ul>



<p><strong>Body Primary</strong></p>

<ul>

&#x20; <li><strong>Desktop:</strong> Regular, 15px</li>

&#x20; <li><strong>Mobile:</strong> Regular, 16sp</li>

&#x20; <li><strong>Usage:</strong> Standard table rows, main form inputs, generic text.</li>

</ul>



<p>Body Secondary</p>

<ul>

&#x20; <li><strong>Desktop:</strong> Regular, 13px</li>

&#x20; <li><strong>Mobile:</strong> Regular, 14sp</li>

&#x20; <li><strong>Usage:</strong> Helper text, list item descriptions, alert descriptions.</li>

</ul>



\---



\## 4. UI Component Architecture \& Consistency Rules

To prevent AI hallucination and ensure absolute visual consistency across all generated screens, all interactive UI components MUST follow these geometric rules:



\* \*\*Text Input Fields (Forms):\*\* \* \*\*Style:\*\* Modern outlined containers. NO floating labels overlapping the top border line. Use inline placeholder text only.

&#x20; \* \*\*Corner Radius:\*\* Strictly `12dp` rounded corners. Never use sharp 0dp corners.

&#x20; \* \*\*Border:\*\* 1px solid stroke in Cool Grey (`#64748B`).

\* \*\*Primary Action Buttons:\*\*

&#x20; \* \*\*Style:\*\* Solid color fill.

&#x20; \* \*\*Corner Radius:\*\* Strictly `12dp` rounded corners.

\* \*\*Data Cards \& Containers:\*\*

&#x20; \* \*\*Style:\*\* Clean geometric boxes with solid fills or subtle 1px borders.

&#x20; \* \*\*Corner Radius:\*\* `16dp` for large grouping cards, `12dp` for smaller item rows.



\---



\## 5. AI Prompting Rules (For Generator Tools)

When generating screens based on this document:

1\. Strictly map system states to the given status color scheme (e.g., if a list item says "Status: In-Transit", the badge background must render using `#7C3AED`).

2\. Adhere unconditionally to the UI Component Architecture rules in Section 4. Ensure all text fields have 12dp rounded corners and no floating labels.

3\. Use modern, abstract geometric layouts with clean borders and subtle spacing. 

4\. Ensure absolute legibility for interactive states across both the mobile transactional view and the JavaFX workspace.

