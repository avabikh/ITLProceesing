/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.example.batch;


/**
 * @author Michael Minella
 */
public class Position {
	private String firm;
	private String account_number;
	private String adp_number;
	private String account_type;
	private String quantity;
	private String holding_type;
	private String quantity_date;
	private String MIPS_client_number;

	public String getFirm() {
		return firm;
	}

	public void setFirm(String firm) {
		this.firm = firm;
	}

	public String getAccount_number() {
		return account_number;
	}

	public void setAccount_number(String account_number) {
		this.account_number = account_number;
	}

	public String getAdp_number() {
		return adp_number;
	}

	public void setAdp_number(String adp_number) {
		this.adp_number = adp_number;
	}

	public String getAccount_type() {
		return account_type;
	}

	public void setAccount_type(String account_type) {
		this.account_type = account_type;
	}

	public String getQuantity() {
		return quantity;
	}

	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}

	public String getHolding_type() {
		return holding_type;
	}

	public void setHolding_type(String holding_type) {
		this.holding_type = holding_type;
	}

	public String getQuantity_date() {
		return quantity_date;
	}

	public void setQuantity_date(String quantity_date) {
		this.quantity_date = quantity_date;
	}

	public String getMIPS_client_number() {
		return MIPS_client_number;
	}

	public void setMIPS_client_number(String MIPS_client_number) {
		this.MIPS_client_number = MIPS_client_number;
	}


}
