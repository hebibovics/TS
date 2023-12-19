import React, { useState, useEffect } from "react";
import "./ProfessorCategoriesPage.css";
import { Button, Form } from "react-bootstrap";
import { useDispatch, useSelector } from "react-redux";
import swal from "sweetalert";
import { useParams } from "react-router-dom";
import * as categoriesConstants from "../../../constants/categoriesConstants";
import FormContainer from "../../../components/FormContainer";
import Sidebar from "../../../components/SidebarProfessor";
import { updateCategory } from "../../../actions/categoriesActions";
import { useNavigate } from "react-router-dom";

const ProfessorSubjectID = () => {
    const params = useParams();
    const subjectId = params.catId;

  
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState(""
    );
    const token = localStorage.getItem("jwtToken");
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [profesorName, setProfesorName] = useState("");


    useEffect(() => {
        const fetchSelectedCategory = async () => {
            try {
                const response = await fetch('/api/subject/professor', {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        "Content-Type": "application/json",
                    },
                });
                if (response.ok) {
                    const selectedCategoryData = await response.json();
                    setTitle(selectedCategoryData.title);
                    setDescription(selectedCategoryData.description);
                    setProfesorName(
                        selectedCategoryData.professor.firstName +
                        " " +
                        selectedCategoryData.professor.lastName
                    );
                    setSelectedUsers(selectedCategoryData.students);
                } else {
                    throw new Error("Failed to fetch selected subject");
                }
            } catch (error) {
                console.error("Error fetching selected subject:", error);
            }
        };

        fetchSelectedCategory();
    }, [subjectId, token]);



    return (
        <div className="adminUpdateCategoryPage__container">
            <div className="adminUpdateCategoryPage__sidebar">
                <Sidebar />
            </div>
            <div className="adminUpdateCategoryPage__content">
                <div>
                    <h2>Subject Details</h2>
                    <p><strong>Subject Name:</strong> {title}</p>
                    <p><strong>Description:</strong> {description}</p>
                    <p><strong>Professor:</strong> {profesorName}</p>
                    <ul><strong>Students:</strong>
                        <li></li>
                    </ul>

                </div>
            </div>
        </div>
    );
};

export default ProfessorSubjectID;
