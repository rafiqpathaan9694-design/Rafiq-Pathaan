package com.example.domain

object FormOptions {
    val genders = listOf("Male", "Female")

    val categories = listOf(
        "General (UR)",
        "OBC-NCL",
        "EWS",
        "SC",
        "ST"
    )

    val religions = listOf(
        "Hindu",
        "Islam",
        "Christian",
        "Sikh",
        "Buddhist",
        "Jain",
        "Parsi",
        "Other"
    )

    val yesNo = listOf("No", "Yes")

    val categoryCertOptions = listOf(
        "Available (Central Format)",
        "Available (State Format)",
        "Applied For / Acknowledgement",
        "Not Applicable (General)"
    )

    val questionMediums = listOf("English", "Hindi")

    val passingYears = (2027 downTo 2012).map { it.toString() }

    val boards = listOf(
        "CBSE (Central Board of Secondary Education)",
        "RBSE (Board of Secondary Education, Rajasthan)",
        "CISCE / ICSE / ISC",
        "UPMSP (UP Board)",
        "MPBSE (MP Board)",
        "BSEB (Bihar Board)",
        "HBSE (Haryana Board)",
        "GSEB (Gujarat Board)",
        "MSBSHSE (Maharashtra Board)",
        "NIOS (National Institute of Open Schooling)",
        "Other State / Recognized Board"
    )

    val annualIncomeRanges = listOf(
        "Up to ₹1,00,000",
        "₹1,00,001 – ₹2,50,000",
        "₹2,50,001 – ₹5,00,000",
        "₹5,00,001 – ₹8,00,000",
        "Above ₹8,00,000"
    )

    val qualifications = listOf(
        "Illiterate",
        "Up to 8th / Middle",
        "10th / Matriculation",
        "12th / Higher Secondary",
        "Graduate",
        "Post Graduate",
        "Doctorate / Professional",
        "Other"
    )

    val occupations = listOf(
        "Agriculture / Farmer",
        "Government Service",
        "Private Service",
        "Business / Self-Employed",
        "Teaching / Academic",
        "Medical / Healthcare",
        "Engineering / Technical",
        "Homemaker",
        "Daily Wage / Labour",
        "Retired",
        "Other"
    )

    val indianStates = listOf(
        "Rajasthan",
        "Uttar Pradesh",
        "Madhya Pradesh",
        "Delhi (NCT)",
        "Haryana",
        "Gujarat",
        "Maharashtra",
        "Bihar",
        "Punjab",
        "Uttarakhand",
        "Andhra Pradesh",
        "Arunachal Pradesh",
        "Assam",
        "Chhattisgarh",
        "Goa",
        "Himachal Pradesh",
        "Jharkhand",
        "Karnataka",
        "Kerala",
        "Manipur",
        "Meghalaya",
        "Mizoram",
        "Nagaland",
        "Odisha",
        "Sikkim",
        "Tamil Nadu",
        "Telangana",
        "Tripura",
        "West Bengal",
        "Andaman & Nicobar Islands",
        "Chandigarh",
        "Dadra & Nagar Haveli and Daman & Diu",
        "Jammu & Kashmir",
        "Ladakh",
        "Lakshadweep",
        "Puducherry"
    )

    val popularExamCities = listOf(
        "Jaipur (RJ)",
        "Kota (RJ)",
        "Sikar (RJ)",
        "Jodhpur (RJ)",
        "Udaipur (RJ)",
        "Ajmer (RJ)",
        "Alwar (RJ)",
        "Bikaner (RJ)",
        "Bharatpur (RJ)",
        "Bhilwara (RJ)",
        "Jhunjhunu (RJ)",
        "Churu (RJ)",
        "Nagaur (RJ)",
        "Pali (RJ)",
        "Sri Ganganagar (RJ)",
        "New Delhi (DL)",
        "Gurugram (HR)",
        "Noida (UP)",
        "Agra (UP)",
        "Lucknow (UP)",
        "Varanasi (UP)",
        "Kanpur (UP)",
        "Prayagraj (UP)",
        "Indore (MP)",
        "Bhopal (MP)",
        "Gwalior (MP)",
        "Ahmedabad (GJ)",
        "Surat (GJ)",
        "Vadodara (GJ)",
        "Mumbai (MH)",
        "Pune (MH)",
        "Nagpur (MH)",
        "Patna (BR)",
        "Chandigarh (CH)",
        "Dehradun (UK)",
        "Bengaluru (KA)",
        "Hyderabad (TS)",
        "Chennai (TN)",
        "Kolkata (WB)"
    )
}
