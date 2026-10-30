export interface Address {
  residentialStreet: string;
  residentialCity: string;
  residentialState: string;
  residentialPostalCode: string;
  residentialCountry: string;
  mailingStreet: string;
  mailingCity: string;
  mailingState: string;
  mailingPostalCode: string;
  mailingCountry: string;
}

export interface EmployeeProfile {
  employeeId: string;
  legalName: string;
  department: string;
  designation: string;
  joiningDate: string;
  managerName: string | null;
  address: Address | null;
}
